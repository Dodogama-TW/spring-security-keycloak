package com.example.springsecuritykeycloakdemo.controller

import arrow.core.flatMap
import com.example.springsecuritykeycloakdemo.helper.ProblemBuilder
import com.example.springsecuritykeycloakdemo.service.TokenService
import com.example.springsecuritykeycloakdemo.utils.JsonSerializationUtils.encodeToStringEither
import com.example.springsecuritykeycloakdemo.utils.JsonSerializationUtils.jsonDefaultSetting
import com.example.springsecuritykeycloakdemo.utils.LogFormat
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@CrossOrigin
@RestController
class UserInfoController(
    private val tokenService: TokenService,
    private val problemBuilder: ProblemBuilder,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @GetMapping("/api/v1/user-info")
    fun getUserInfo(
        request: HttpServletRequest,
        authentication: OAuth2AuthenticationToken,
    ): ResponseEntity<String> =
        logger.info(LogFormat.GET_REQUEST_LOG_FORMAT, request.requestURI).let {
            tokenService.getTestUserInfo(auth = authentication)
        }.flatMap { testUserInfo ->
            jsonDefaultSetting.encodeToStringEither(testUserInfo)
        }.fold(
            ifLeft = { err ->
                problemBuilder.problemDetailBuilder(applicationError = err).let { problemDetailResponseBody ->
                    ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.parseMediaType("application/problem+json"))
                        .body(problemDetailResponseBody)
                }
            },
            ifRight = { responseBodyString ->
                ResponseEntity
                    .status(HttpStatus.OK)
                    .contentType(MediaType.parseMediaType("application/json"))
                    .body(responseBodyString)
            },
        )
}
