package com.example.springsecuritykeycloakdemo.model

import kotlinx.serialization.Serializable

@Serializable
data class ProblemDetail(
    val status: String,
    val detail: String
)
