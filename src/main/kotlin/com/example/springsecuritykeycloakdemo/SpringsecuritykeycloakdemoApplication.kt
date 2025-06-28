package com.example.springsecuritykeycloakdemo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan("com.example.springsecuritykeycloakdemo.config")
class SpringSecurityKeycloakDemoApplication

fun main(args: Array<String>) {
    runApplication<SpringSecurityKeycloakDemoApplication>(*args)
}
