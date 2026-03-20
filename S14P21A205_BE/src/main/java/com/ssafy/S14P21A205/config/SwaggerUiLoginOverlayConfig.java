package com.ssafy.S14P21A205.config;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.webmvc.ui.SwaggerIndexPageTransformer;
import org.springdoc.webmvc.ui.SwaggerIndexTransformer;
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.resource.ResourceTransformerChain;
import org.springframework.web.servlet.resource.TransformedResource;

/** 용도: Swagger UI 상단에 로그인 오버레이(버튼 바) 주입. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "springdoc.swagger-ui.enabled", matchIfMissing = true)
public class SwaggerUiLoginOverlayConfig {

        /** 용도: Swagger index HTML 변환기 등록. */
    @Bean
    public SwaggerIndexTransformer swaggerIndexTransformer(
            SwaggerUiConfigProperties swaggerUiConfigProperties,
            SwaggerUiOAuthProperties swaggerUiOAuthProperties,
            SwaggerWelcomeCommon swaggerWelcomeCommon,
            ObjectMapperProvider objectMapperProvider
    ) {
        return new SwaggerIndexPageTransformer(
                swaggerUiConfigProperties,
                swaggerUiOAuthProperties,
                swaggerWelcomeCommon,
                objectMapperProvider
        ) {
            @Override
            public @NonNull Resource transform(
                    @NonNull HttpServletRequest request,
                    @NonNull Resource resource,
                    @NonNull ResourceTransformerChain transformerChain
            ) throws IOException {
                Resource transformed = super.transform(request, resource, transformerChain);
                String html = new String(transformed.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                String injected = injectLoginBar(html);
                if (injected.equals(html)) {
                    return transformed;
                }
                return new TransformedResource(resource, injected.getBytes(StandardCharsets.UTF_8));
            }
        };
    }

    private static String injectLoginBar(String html) {
        String marker = "<div id=\"swagger-ui\"></div>";
        int idx = html.indexOf(marker);
        if (idx < 0) {
            return html;
        }

        return html.substring(0, idx)
                + swaggerLoginBarHtml()
                + html.substring(idx);
    }

    private static String swaggerLoginBarHtml() {
        return """
                <div id="swagger-auth-bar" style="position: sticky; top: 0; z-index: 9999; background: #111; color: #fff; padding: 10px 12px; font-family: ui-sans-serif, system-ui, -apple-system, Segoe UI, Roboto, Helvetica, Arial, 'Apple SD Gothic Neo', 'Noto Sans KR'; display: flex; gap: 10px; align-items: center;">
                  <a href="/auth/login?redirect=/swagger-ui/index.html" style="display: inline-flex; align-items: center; gap: 8px; background: #4285F4; color: #fff; border-radius: 10px; padding: 8px 10px; text-decoration: none; font-size: 13px; font-weight: 600;">
                    Google 로그인
                  </a>
                  <a href="/auth/login?provider=ssafy&redirect=/swagger-ui/index.html" style="display: inline-flex; align-items: center; gap: 8px; background: #00A86B; color: #fff; border-radius: 10px; padding: 8px 10px; text-decoration: none; font-size: 13px; font-weight: 600;">
                    SSAFY 로그인
                  </a>
                  <button id="swagger-auth-me" type="button" style="background: #fff; color: #111; border: 1px solid rgba(255,255,255,0.15); border-radius: 10px; padding: 8px 10px; font-size: 13px; cursor: pointer;">
                    내 정보
                  </button>
                  <button id="swagger-auth-refresh" type="button" style="background: #f6c344; color: #111; border: 1px solid rgba(255,255,255,0.15); border-radius: 10px; padding: 8px 10px; font-size: 13px; cursor: pointer;">
                    토큰 재발급
                  </button>
                  <button id="swagger-auth-logout" type="button" style="background: transparent; color: #fff; border: 1px solid rgba(255,255,255,0.25); border-radius: 10px; padding: 8px 10px; font-size: 13px; cursor: pointer;">
                    Logout
                  </button>
                  <span style="margin-left: auto; opacity: 0.85; font-size: 12px;">
                    Access JWT + HttpOnly refresh 쿠키
                  </span>
                </div>
                <script>
                  (() => {
                    const ACCESS_TOKEN_KEY = "swaggerAccessToken";
                    const params = new URLSearchParams(window.location.search);

                    const getAccessToken = () => window.localStorage.getItem(ACCESS_TOKEN_KEY);
                    const setTokens = (accessToken) => {
                      if (accessToken) {
                        window.localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
                      }
                    };
                    const clearTokens = () => {
                      window.localStorage.removeItem(ACCESS_TOKEN_KEY);
                    };
                    const stripHash = () => window.history.replaceState({}, "", window.location.pathname + window.location.search);
                    const saveTokensFromHash = () => {
                      const rawHash = window.location.hash.startsWith("#")
                        ? window.location.hash.substring(1)
                        : window.location.hash;
                      if (!rawHash) {
                        return;
                      }
                      const hashParams = new URLSearchParams(rawHash);
                      const accessToken = hashParams.get("accessToken");
                      if (accessToken) {
                        setTokens(accessToken);
                        stripHash();
                      }
                    };
                    const applySwaggerToken = () => {
                      const accessToken = getAccessToken();
                      if (!accessToken || !window.ui || !window.ui.preauthorizeApiKey) {
                        return false;
                      }
                      try {
                        window.ui.preauthorizeApiKey("bearerAuth", accessToken);
                        return true;
                      } catch (error) {
                        return false;
                      }
                    };

                    const loginError = params.get("loginError");
                    if (loginError) {
                      const loginErrorDescription = params.get("loginErrorDescription");
                      const message = loginErrorDescription
                        ? "로그인 실패: " + loginError + "\\n" + loginErrorDescription
                        : "로그인 실패: " + loginError;
                      window.setTimeout(() => alert(message), 100);
                      params.delete("loginError");
                      params.delete("loginErrorDescription");
                      const nextQuery = params.toString();
                      const nextUrl = nextQuery
                        ? window.location.pathname + "?" + nextQuery + window.location.hash
                        : window.location.pathname + window.location.hash;
                      window.history.replaceState({}, "", nextUrl);
                    }

                    saveTokensFromHash();

                    let tries = 0;
                    const interval = window.setInterval(() => {
                      tries += 1;
                      if (applySwaggerToken() || tries > 40) {
                        window.clearInterval(interval);
                      }
                    }, 500);

                    const request = (url, method = "GET", body) => {
                      const headers = {
                        "Accept": "application/json"
                      };
                      const accessToken = getAccessToken();
                      if (accessToken) {
                        headers["Authorization"] = "Bearer " + accessToken;
                      }
                      if (body !== undefined) {
                        headers["Content-Type"] = "application/json";
                      }
                      return fetch(url, {
                        method,
                        credentials: "include",
                        headers,
                        body: body === undefined ? undefined : JSON.stringify(body)
                      });
                    };

                    const meBtn = document.getElementById("swagger-auth-me");
                    if (meBtn) {
                      meBtn.addEventListener("click", async () => {
                        const accessToken = getAccessToken();
                        if (!accessToken) {
                          alert("로그인 후 다시 시도해주세요.");
                          return;
                        }
                        const response = await request("/users");
                        if (!response.ok) {
                          alert("인증 상태를 확인해주세요. HTTP " + response.status);
                          return;
                        }
                        const body = await response.json();
                        alert(JSON.stringify(body, null, 2));
                      });
                    }

                    const refreshBtn = document.getElementById("swagger-auth-refresh");
                    if (refreshBtn) {
                      refreshBtn.addEventListener("click", async () => {
                        const response = await request("/auth/refresh", "POST");
                        if (!response.ok) {
                          alert("토큰 재발급에 실패했습니다. HTTP " + response.status);
                          return;
                        }
                        const body = await response.json();
                        setTokens(body.accessToken);
                        applySwaggerToken();
                        alert("토큰 재발급 완료");
                      });
                    }

                    const logoutBtn = document.getElementById("swagger-auth-logout");
                    if (logoutBtn) {
                      logoutBtn.addEventListener("click", async () => {
                        await request("/auth/logout", "POST");
                        clearTokens();
                        window.location.reload();
                      });
                    }
                  })();
                </script>
                """;
    }
}
