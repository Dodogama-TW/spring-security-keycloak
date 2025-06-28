package com.example.springsecuritykeycloakdemo.helper

import com.example.springsecuritykeycloakdemo.config.KeycloakConfig
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler
import org.springframework.stereotype.Component

@Component
class KeycloakLogoutHandler(
    private val keycloakConfig: KeycloakConfig
): LogoutSuccessHandler {

    override fun onLogoutSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication?
    ) {
        val keycloakLogoutUri =
            "${keycloakConfig.logoutUrl}?redirect_uri=${keycloakConfig.redirectUrl}"
        response.sendRedirect(keycloakLogoutUri)
    }

}