package com.example.springsecuritykeycloakdemo.config

import com.example.springsecuritykeycloakdemo.helper.KeycloakLogoutHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SecurityConfig(
    private val keycloakLogoutHandler: KeycloakLogoutHandler
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { auth ->
                auth.requestMatchers("/", "/webjars/**", "/css/**", "/js/**").permitAll()
                    .anyRequest().authenticated()
            }
            .oauth2Login { Customizer.withDefaults<HttpSecurity>() }
            .logout { logout ->
                logout.logoutUrl("/logout")
                    .logoutSuccessHandler(keycloakLogoutHandler)
                    .invalidateHttpSession(true)
                    .clearAuthentication(true)
                    .deleteCookies("JSESSIONID")
            }
        return http.build()
    }
}
