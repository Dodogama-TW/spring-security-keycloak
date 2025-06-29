# Add Oauth2 Authentication to your Spring Boot Project Example

> :bulb: 剛剛碰Oauth2 的菜鳥工程師，如果有任何有敘述錯誤的地方，請不吝色指教 (｡A｡)

## System Architecture
![system](/system-architecture/system-design.png)

## Data Flow
![data flow](/system-architecture/dataflow.png)

## Description
這個範例主要用於實作 Spring Boot 怎麼與 Oauth 2.0 Server 串接，實現登入用戶的認證 `Authentication`。
為了提升可以設定的彈性，這邊使用Keycloak 作為我們的 Oauth 2.0 Server。可以自行設定Token 的 Lifecycle，以及Session Idle Time 等等。請依據你的專案情境進行調整。

## Code 重點整理
1. dependency
```xml
<!-- Spring Boot 基礎 -->
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-web</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-validation</artifactId>
		</dependency>

		<!-- Spring Security OAuth2 Login -->
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-oauth2-client</artifactId>
		</dependency>

		<!-- 用於後端呼叫其他 OAuth API（例如 Google API） -->
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-webflux</artifactId>
		</dependency>
```

2. `/config/SecurityConfig` 這裡是關鍵，用於設定怎麼Login和Logout。這裡除了允許一些路徑不用檢查外，其他一律要進行認證。我們透過 `oauth2Login { Customizer.withDefaults<HttpSecurity>() }` 讓系統自動去讀取 `application.yaml` 裡 `spring.security`底下的oauth2設定，建立oauth2 login。 <br> Logout這邊為收到 `/logout` 請求時會去執行 `keycloakLogoutHandler` redirect 到 keycloak logout page。並且一次程序完成後，刪除 SecurityContextHolder內的 authentication 資料，並清除前端用戶的cookie, session資料。

> :bulb: 如果Oauth 2 Login有成功執行，`springframework.security`會自動把`authentication` 資訊寫入 `SecurityContextHolder`內，並且這些 `authentication` 資訊可以直接帶到 controller 的 function 內使用。

```kt
@Configuration
class SecurityConfig(
    private val keycloakLogoutHandler: KeycloakLogoutHandler,
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
        return http.build()
    }
}
```

3. `/helper/KeycloakLogoutHandler` 這邊用來設定怎麼 redirect 到 keycloak 去執行logout
```kt
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
```

4. Controller 可以直接取得 authentication 內容，如果當前request是 **Unauthenticated** (以這個專案來說就是沒走Oauth 2 Login) 或是 **Refresh Token Expired** 那這個 authentication 就會 `null`

```kt
    @GetMapping("/api/v1/user-info")
    fun getUserInfo(
        request: HttpServletRequest,
        authentication: OAuth2AuthenticationToken,
    ): ResponseEntity<String> =
        logger.info(LogFormat.GET_REQUEST_LOG_FORMAT, request.requestURI).let {
            tokenService.getTestUserInfo(auth = authentication)
        }.flatMap { testUserInfo ->
        ...
```

5. ID token 的解析可以參考 `/service/TokenService` 的寫法，我們設定Keycloak是符合OIDC，並且在 `application.yaml` 的 `keycloak.scope` 內有設定為 `openid`，所以理論上拿到的會是`OidcUser`這個type。透過`attributes`我們可以直接拿到ID token內特定欄位的資料，**不需要再額外去解JWT看裡面的內容**。

```kt
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
                TokenError(reason = "Unexpected oauth2 user type. ${auth.principal::class.simpleName}").left()
            }
        }

    fun getIdToken(auth: OAuth2AuthenticationToken): Either<ApplicationError, OidcIdToken> =
        auth.principal.let { oAuth2User ->
            when (oAuth2User) {
                is OidcUser -> {
                    oAuth2User.idToken.right()
                }
                else -> {
                    TokenError(reason = "Unexpected oauth2 user type. ${oAuth2User::class.simpleName}").left()
                }
            }
        }
}
```

## Preparation

### 安裝 Keycloak
為了安裝的方便性這邊使用Docker進行安裝，請先確認你的電腦已經安裝好Docker Desktop / Docker Engine。 [官方網站安裝教學](https://docs.docker.com/desktop/)

透過Docker運行Keycloak請參考這篇官方教學。 [官方教學](https://www.keycloak.org/getting-started/getting-started-docker)

1. 先確認docker是否安裝
```cmd
docker -v
```

2. 在自己的電腦執行docker keycloak
> :bulb: 密碼請自行調整，這邊是測試用直接設定admin。其餘設定請依據需求調整

```cmd
docker run -p 8080:8080 -e KC_BOOTSTRAP_ADMIN_USERNAME=admin -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin quay.io/keycloak/keycloak:26.2.5 start-dev
```

3. 透過 `http://localhost:8080/` 登入Keycloak admin console
4. 按照官方教學完成 [create realm](https://www.keycloak.org/getting-started/getting-started-docker#_create_a_realm), [create user](https://www.keycloak.org/getting-started/getting-started-docker#_create_a_user), [create client](https://www.keycloak.org/getting-started/getting-started-docker#_secure_the_first_application)。以下用簡單的舉例這三個項目的差異。
   - realm : 國家，底下會有多個縣市 (**client**)，可以為國民 (**user**)訂定基本規則
   - client : 縣市，可以直接使用自己的規則，也可以依需求自訂規則。並且可以查到國內 (**realm**)國民 (**user**)的資訊，確認他是合法 (**authentication**) 的國民 (**user**)
   - user : 國民，只要在國內 (**realm**)，可以到各個縣市去 (**client**) 玩
5. 除了官方教學，這邊還會進行額外設定
   - client scope : 需要額外在 ID token 內顯示的欄位可以新增，並加到需要的client底下。 這邊新增 `test-account-name`
       
      1. 在 `Manage realms` 內選擇你的 `realm`。並確認左上current realm內顯示的是否正確
      2. 在 `Client scopes` 按下 `Create client scope`，並設定你要的 `Name` 按下 Save
      3. 在下面的列表找到你剛剛創建的 `client scope`，在上面分頁找到`Mapper`，進去找到 `Add Mapper`，選擇你這個`client scope` 的值要怎麼設定。這裡使用By Configuration -> User Attribute，便完成以下設定，把這個加到ID token內。
      ![alt text](/image/client-scope-setting.png)
      4. 完成後，左邊選單選擇 `Clients`，找到你要的 `client`，在 `client scopes` 分頁選擇 `Add client scope` 幫我們剛剛新增的 `client scope` 加進去。
   - session : 調整 access token 和 refresh token 的 expired time

     1. 左邊選單選擇 `Realm setting`，切到 `Session` 分頁，這裡可以調整session的lifecycle，也就是 refresh token 的 lifecycle
     ![alt text](/image/sso-session-setting.png)
     2. 切到 `Tokens` 分頁，這裡可以調整 access token 的 lifecycle。為了方便測試這裡刻意調短lifecycle時間。
     ![alt text](/image/access-token-setting.png)

### 如何啟動這個 Spring Boot 3 程式?
1. 設定 .env ， 可以直接複製 `/src/main/application.properties` 的內容
2. 依據你 keycloak 的設定調整 `application.yml` 內的設定
3. 執行 springboot:run，記得有把 .env 設定上去喔 
   > :bulb: 我是用 intellij 的 run maven configuration，搭配 env file plugin。如果是使用mvn的再麻煩自行查找怎麼設定

## 常見問題
1. Keycloak 的登出問題
![keycloak-logout](/image/keycloak-logout-question.png)

2. 清除 Authentication 的時機點
![clear authentication](/image/authentication-clear-timing.png)

3. 為什麼要Disable CSRF? (我菜還想不到怎麼解決CSRF இдஇ)
![disable csrf](/image/why-disable-csrf.png)

4. 開發中可能遇到CORS問題 (前端測試網頁之後有空再改 (´;ω;`) )
![cors problem](/image/normal-cors-problem.png)