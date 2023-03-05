package com.github.jbokman.intellijpluginstreamdeck

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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.Duration

class MyApplicationService : StartupActivity {
    override fun runActivity(project: Project) {
        println(MyBundle.message("applicationService"))
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
            }
        }
    }
}
