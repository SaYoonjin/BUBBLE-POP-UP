package com.ssafy.S14P21A205.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 용도: OpenAPI(Swagger) 메타 정보 설정. */
@Configuration
public class OpenApiConfig {

    /** 용도: OpenAPI 문서 기본 정보 생성. */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes(
                        "bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                ))
                .info(new Info()
                        .title("S14P21A205 API")
                        .version("v1")
                        .description("Swagger 상단의 OAuth 로그인 버튼으로 JWT를 발급받은 뒤 Try it out을 사용할 수 있습니다."));
    }
}
