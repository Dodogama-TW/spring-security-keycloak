package com.example.springsecuritykeycloakdemo.helper

import com.example.springsecuritykeycloakdemo.model.ApplicationError
import com.example.springsecuritykeycloakdemo.model.GetTestUserInfoError
import com.example.springsecuritykeycloakdemo.model.SerializationError
import com.example.springsecuritykeycloakdemo.model.TokenError
import org.springframework.stereotype.Component

@Component
class ProblemBuilder {
    fun problemDetailBuilder(applicationError: ApplicationError): String =
        when (applicationError) {
            is SerializationError ->
                """
                {
                  "status": "SerializationError-500",
                  "detail": "${applicationError.e.message.orEmpty()}"
                }
                """.trimIndent()

            is TokenError ->
                """
                {
                  "status": "TokenError-500",
                  "detail": "${applicationError.reason}"
                }
                """.trimIndent()
            is GetTestUserInfoError ->
                """
                {
                  "status": "GetTestUserInfoError-500",
                  "detail": "${applicationError.reason}"
                }
                """.trimIndent()
        }
}
