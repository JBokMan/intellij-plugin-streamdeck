package com.github.jbokman.intellijpluginstreamdeck

import com.github.jbokman.intellijpluginstreamdeck.authentication.SecretManager
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import io.ktor.server.auth.basic
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.send
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory


class MyApplicationService : StartupActivity {
    companion object {
        val secretManager: SecretManager = SecretManager()
    }

    override fun runActivity(project: Project) {
        val scope = CoroutineScope(Dispatchers.Default)

        val environment = applicationEngineEnvironment {
            log = LoggerFactory.getLogger("ktor.application")
            connector {
                port = 12345
            }
            module(Application::myApplicationModule)
        }
        scope.launch {
            embeddedServer(Netty, environment).start(wait = true)
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
    authentication {
        basic(name = "tokenAuth") {
            realm = "Ktor Server"
            validate { credentials ->
                MyApplicationService.secretManager.validateToken(credentials.name + credentials.password)
            }
        }
    }
    routing {
        authenticate(configurations = arrayOf("tokenAuth"), optional = false) {
            webSocket("/") {
                performDeleteLineAction()
            }
        }
    }
}

fun performDeleteLineAction() {
    val actionManager = ActionManager.getInstance()
    val action = actionManager.getAction(IdeActions.ACTION_EDITOR_DELETE_LINE)
    val dataContext = DataManager.getInstance().dataContextFromFocusAsync.blockingGet(5000)
        ?: return

    val inputEvent = KeyEvent(
        LangDataKeys.EDITOR.getData(dataContext)?.contentComponent, // the component that the event originated from
        KeyEvent.KEY_PRESSED, // the type of event (pressed, released, typed)
        System.currentTimeMillis(), // the time the event occurred
        InputEvent.META_DOWN_MASK, // any modifiers (shift, control, alt, cmd/windows)
        KeyEvent.VK_DELETE, // the key code of the pressed key
        KeyEvent.CHAR_UNDEFINED // the character representation of the key (undefined for non-character keys)
    )
    val event = AnActionEvent.createFromAnAction(
        action, // the action to be triggered
        inputEvent, // the input event that triggered the action
        ActionPlaces.UNKNOWN, // the place (e.g. the toolbar or menu) where the action was triggered
        dataContext, // the context of the action (e.g. the selected text or file)
    )

    ApplicationManager.getApplication().invokeAndWait {
        ApplicationManager.getApplication().runWriteAction {
            action.actionPerformed(event)
        }
    }
}
