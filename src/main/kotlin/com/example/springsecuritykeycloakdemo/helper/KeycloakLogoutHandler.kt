package com.example.springsecuritykeycloakdemo.helper

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.toOption
import com.example.springsecuritykeycloakdemo.config.KeycloakConfig
import com.example.springsecuritykeycloakdemo.model.TokenError
import com.example.springsecuritykeycloakdemo.service.TokenService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler
import org.springframework.stereotype.Component

@Component
class KeycloakLogoutHandler(
    private val keycloakConfig: KeycloakConfig,
    private val tokenService: TokenService,
) : LogoutSuccessHandler {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun onLogoutSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication?,
    ) {
        authentication.toOption().toEither {
            logger.error("Failed to get id token when logout. Reason: unauthenticated.")
            TokenError(reason = "Failed to get id token when logout. Reason: unauthenticated.")
        }.map {
            it as OAuth2AuthenticationToken
        }.flatMap { oAuth2AuthenticationToken ->
            tokenService.getIdToken(auth = oAuth2AuthenticationToken)
        }.map { oidcIdToken ->
            "${keycloakConfig.logoutUrl}?id_token_hint=${oidcIdToken.tokenValue}&post_logout_redirect_uri=${keycloakConfig.redirectUrl}"
        }.flatMap { keycloakLogoutUri ->
            Either.catch {
                // ✅ 加上 CORS headers
                response.setHeader("Access-Control-Allow-Origin", keycloakConfig.redirectUrl)
                response.setHeader("Access-Control-Allow-Credentials", "true")
                response.sendRedirect(keycloakLogoutUri)
            }.mapLeft { err ->
                logger.error("Error occurred when redirect to keycloak logout: $err")
            }.tap {
                logger.info("logout successfully")
                SecurityContextHolder.clearContext()
            }
        }
    }
}
