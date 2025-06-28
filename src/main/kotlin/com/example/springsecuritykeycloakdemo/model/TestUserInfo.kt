package com.example.springsecuritykeycloakdemo.model

import kotlinx.serialization.Serializable

@Serializable
data class TestUserInfo(
    val userName: String,
    val testAccountName: String,
)
