package com.github.jbokman.intellijpluginstreamdeck.toolWindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class TokenToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.SERVICE.getInstance()
        val tokenToolWindow = TokenToolWindow()
        val content = contentFactory.createContent(tokenToolWindow.getComponent(), "", false)
        toolWindow.contentManager.addContent(content)
    }
}