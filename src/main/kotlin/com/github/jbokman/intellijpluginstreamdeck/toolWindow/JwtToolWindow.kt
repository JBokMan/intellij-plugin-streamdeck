package com.github.jbokman.intellijpluginstreamdeck.toolWindow

import com.github.jbokman.intellijpluginstreamdeck.MyApplicationService
import com.intellij.ui.layout.CCFlags
import com.intellij.ui.layout.panel
import javax.swing.JComponent
import javax.swing.JTextField

class JwtToolWindow {
    private var jwtTokenField = JTextField(MyApplicationService.secretManager.getToken())

    private fun generateNewToken() {
        val newToken = MyApplicationService.secretManager.generateNewToken()
        jwtTokenField.text = newToken
    }

    fun getComponent(): JComponent {
        return panel {
            row {
                label("JWT Token: ", bold = true)
            }
            row {
                cell(isFullWidth = true) {
                    jwtTokenField(CCFlags.growX, CCFlags.pushX)
                    button("Generate New Token") {
                        generateNewToken()
                    }
                }
            }
        }
    }
}

