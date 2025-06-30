package com.example.springsecuritykeycloakdemo.controller

import com.example.springsecuritykeycloakdemo.utils.testdata.UserInfoControllerTestData
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class UserInfoControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    @DisplayName(
        """
            GIVEN a valid user which complete oauth2 login
             WHEN calling /api/v1/user-info
             THEN it should get http status 200
              AND get user information which userName [DODOGAMA TW]
                AND testAccountName [dodogamatw]
        """,
    )
    fun `test get user information api successfully`() {
        mockMvc.perform(
            get("/api/v1/user-info")
                .with(
                    SecurityMockMvcRequestPostProcessors.authentication(
                        UserInfoControllerTestData.testAuthentication,
                    ),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(content().json(UserInfoControllerTestData.expectedTestUserInfoResponse))
    }

    @Test
    @DisplayName(
        """
            GIVEN request without oauth2 login session
             WHEN calling /api/v1/user-info
             THEN it should get http status 302
              AND redirect to oauth2 login
        """,
    )
    fun `test get user information api redirect to oauth2Login`() {
        mockMvc.perform(get("/api/v1/user-info"))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrlPattern("**/oauth2/authorization/**"))
    }
}
