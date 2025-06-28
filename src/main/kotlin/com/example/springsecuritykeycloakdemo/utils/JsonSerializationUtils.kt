package com.example.springsecuritykeycloakdemo.utils

import arrow.core.Either
import com.example.springsecuritykeycloakdemo.model.ApplicationError
import com.example.springsecuritykeycloakdemo.model.SerializationError
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

object JsonSerializationUtils {
    val jsonIgnoreUnknownKeys = Json { ignoreUnknownKeys = true }
    val jsonDefaultSetting = Json { encodeDefaults = true }

    inline fun <reified T> Json.encodeToStringEither(value: T): Either<ApplicationError, String> =
        Either.catch {
            this.encodeToString(value = value, serializer = serializer())
        }.mapLeft { err ->
            SerializationError(e = err)
        }

    inline fun <reified T> Json.decodeFromStringEither(value: String): Either<ApplicationError, T> =
        Either.catch {
            this.decodeFromString<T>(value)
        }.mapLeft { err ->
            SerializationError(e = err)
        }
}
