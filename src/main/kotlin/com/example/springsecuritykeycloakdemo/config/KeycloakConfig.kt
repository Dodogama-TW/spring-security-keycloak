package com.example.springsecuritykeycloakdemo.config

import org.jetbrains.annotations.NotNull
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "keycloak")
class KeycloakConfig(
    @field:NotNull
    val logoutUrl: String,
    @field:NotNull
    val redirectUrl: String,
)
