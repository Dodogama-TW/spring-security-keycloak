package com.example.springsecuritykeycloakdemo.model

sealed interface ApplicationError

data class TokenError(val reason: String): ApplicationError

data class GetTestUserInfoError(val reason: String): ApplicationError

data class SerializationError(val e: Throwable): ApplicationError
