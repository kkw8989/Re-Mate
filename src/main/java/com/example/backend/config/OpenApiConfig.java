package com.example.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  private static final String BEARER_KEY = "BearerAuth";

  @Bean
  public OpenAPI openAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Re:Mate API")
                .description(
                    """
                                    Re:Mate 백엔드 API 문서입니다.

                                    - 인증이 필요한 API는 우측 상단 Authorize에 Bearer 토큰을 넣고 테스트합니다.
                                    """)
                .version("v1"))
        .addSecurityItem(new SecurityRequirement().addList(BEARER_KEY))
        .components(
            new Components()
                .addSecuritySchemes(
                    BEARER_KEY,
                    new SecurityScheme()
                        .name(BEARER_KEY)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
  }
}
