package com.github.jbokman.intellijpluginstreamdeck.toolWindow

import com.github.jbokman.intellijpluginstreamdeck.MyApplicationService
import com.intellij.ui.layout.CCFlags
import com.intellij.ui.layout.panel
import javax.swing.JComponent
import javax.swing.JTextField

class TokenToolWindow {
    private var authTokenField = JTextField(MyApplicationService.secretManager.getToken().value)

    private fun generateNewToken() {
        val newToken = MyApplicationService.secretManager.generateNewToken()
        authTokenField.text = newToken.value
    }

    fun getComponent(): JComponent {
        return panel {
            row {
                label("Auth Token: ", bold = true)
            }
            row {
                cell(isFullWidth = true) {
                    authTokenField(CCFlags.growX, CCFlags.pushX)
                    button("Generate New Token") {
                        generateNewToken()
                    }
                }
            }
        }
    }
}

