package com.github.jbokman.intellijpluginstreamdeck

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.time.Duration


class MyApplicationService : StartupActivity {

    override fun runActivity(project: Project) {
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            embeddedServer(Netty, port = 12345, module = Application::myApplicationModule).start(wait = true)
        }
    }
}

fun Application.myApplicationModule() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    routing {
        webSocket("/") {
            send("You are connected!")
            for (frame in incoming) {
                frame as? Frame.Text ?: continue
                val receivedText = frame.readText()
                send("You said: $receivedText")

                send("Start")
                val actionManager = ActionManager.getInstance()
                send(actionManager.toString())
                val action = actionManager.getAction(IdeActions.ACTION_EDITOR_DELETE_LINE)
                send(action.toString())
                val dataContext =
                    DataManager.getInstance().dataContextFromFocusAsync.blockingGet(5000) ?: return@webSocket

                val editor = LangDataKeys.EDITOR.getData(dataContext)
                val component = editor?.contentComponent


                val inputEvent = KeyEvent(
                    component, // the component that the event originated from
                    KeyEvent.KEY_PRESSED, // the type of event (pressed, released, typed)
                    System.currentTimeMillis(), // the time the event occurred
                    InputEvent.SHIFT_DOWN_MASK, // any modifiers (shift, control, alt)
                    KeyEvent.VK_DELETE, // the key code of the pressed key
                    KeyEvent.CHAR_UNDEFINED // the character representation of the key (undefined for non-character keys)
                )

                val event = AnActionEvent.createFromAnAction(
                    action, // the action to be triggered
                    inputEvent, // the input event that triggered the action
                    "null", // the place (e.g. the toolbar or menu) where the action was triggered
                    dataContext, // the context of the action (e.g. the selected text or file)
                )

                send(event.toString())

                ApplicationManager.getApplication().invokeAndWait {
                    ApplicationManager.getApplication().runWriteAction {
                        action.actionPerformed(event)
                    }
                }
            }

            send("Action Performed")
        }
    }
}