package com.example.springsecuritykeycloakdemo.service

import arrow.core.Either
import arrow.core.Option
import arrow.core.left
import arrow.core.toOption
import com.example.springsecuritykeycloakdemo.model.ApplicationError
import com.example.springsecuritykeycloakdemo.model.GetTestUserInfoError
import com.example.springsecuritykeycloakdemo.model.TestUserInfo
import com.example.springsecuritykeycloakdemo.model.TokenError
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.stereotype.Service

@Service
class TokenService(
    private val authorizedClientService: OAuth2AuthorizedClientService,
) {
    fun getTestUserInfo(auth: OAuth2AuthenticationToken): Either<ApplicationError, TestUserInfo> =
        when (auth.principal) {
            is OidcUser -> {
                auth.principal.attributes["name"].toOption().zip(
                    auth.principal.attributes["test-account-name"].toOption(),
                ) { userName, testAccountName ->
                    TestUserInfo(
                        userName = userName as String,
                        testAccountName = testAccountName as String,
                    )
                }.toEither {
                    GetTestUserInfoError(reason = "Failed to get [name] and [test-account-name] from ID token")
                }
            }
            else -> {
                TokenError(reason = "Unexpected oauth 2 user type. ${auth.principal::class.simpleName}").left()
            }
        }

    private fun loadClient(auth: OAuth2AuthenticationToken): Option<OAuth2AuthorizedClient> =
        authorizedClientService.loadAuthorizedClient<OAuth2AuthorizedClient?>(
            auth.authorizedClientRegistrationId,
            auth.name,
        ).toOption()
}
