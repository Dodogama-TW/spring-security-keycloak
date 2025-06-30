package com.example.springsecuritykeycloakdemo.utils.testdata

import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser
import java.time.Instant

object UserInfoControllerTestData {
    private val testValidIdToken: OidcIdToken =
        OidcIdToken(
            "token-for-test",
            Instant.now(),
            Instant.now().plusSeconds(3600),
            mapOf(
                "sub" to "123456789",
                "name" to "DODOGAMA TW",
                "test-account-name" to "dodogamatw",
                "email" to "test@example.com",
            ),
        )

    private val testOidcUser =
        DefaultOidcUser(
            listOf(SimpleGrantedAuthority("ROLE_USER")),
            testValidIdToken,
        )

    val testAuthentication =
        OAuth2AuthenticationToken(
            testOidcUser,
            testOidcUser.authorities,
            "keycloak",
        )

    val expectedTestUserInfoResponse: String =
        """
        {
          "userName":"DODOGAMA TW",
          "testAccountName":"dodogamatw"
        }
        """.trimIndent()
}
