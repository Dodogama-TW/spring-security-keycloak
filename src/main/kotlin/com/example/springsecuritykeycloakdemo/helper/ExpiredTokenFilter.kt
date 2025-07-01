package com.example.springsecuritykeycloakdemo.helper

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.stereotype.Component
import org.springframework.web.filter.GenericFilterBean
import java.time.Instant

@Component
class ExpiredTokenFilter : GenericFilterBean() {
    override fun doFilter(
        request: ServletRequest,
        response: ServletResponse,
        chain: FilterChain,
    ) {
        val context = SecurityContextHolder.getContext()
        val authentication = context.authentication

        if (authentication is OAuth2AuthenticationToken) {
            val principal = authentication.principal
            if (principal is OidcUser) {
                val expiresAt = principal.idToken.expiresAt
                if (expiresAt != null && expiresAt.isBefore(Instant.now())) {
                    logger.warn("Token expired, redirecting to re-authentication")

                    // 清除認證狀態
                    SecurityContextHolder.clearContext()

                    // 重新導向登入（根據你的 registrationId）
                    val registrationId = authentication.authorizedClientRegistrationId
                    val httpResp = response as HttpServletResponse

                    httpResp.sendRedirect("http://localhost:8080/oauth2/authorization/$registrationId")
                    return
                }
            }
        }

        // token 尚未過期，繼續後續流程
        chain.doFilter(request, response)
    }
}
