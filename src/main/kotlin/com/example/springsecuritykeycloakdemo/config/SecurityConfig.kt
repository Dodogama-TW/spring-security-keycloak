package com.example.springsecuritykeycloakdemo.config

import com.example.springsecuritykeycloakdemo.helper.ExpiredTokenFilter
import com.example.springsecuritykeycloakdemo.helper.KeycloakLogoutHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
class SecurityConfig(
    private val keycloakLogoutHandler: KeycloakLogoutHandler,
    private val expiredTokenFilter: ExpiredTokenFilter
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { csrf ->
                csrf.disable()
            }
            .authorizeHttpRequests { auth ->
                auth.requestMatchers("/webjars/**", "/css/**", "/js/**").permitAll()
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
            .addFilterBefore(expiredTokenFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }
}
