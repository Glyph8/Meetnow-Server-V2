package timetogeter.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes("BearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .info(apiInfo())
//                .servers(List.of(new Server().url("https://meetnow.duckdns.org"))); // http://localhost:8080
                .servers(List.of(new Server().url("http://localhost:8080"))); //
    }

    private Info apiInfo() {
        return new Info()
                .title("타임투게더 API")
                .description("개인정보 보호 및 사용자 맞춤형 일정 관리 서비스")
                .version("1.0.0");
    }
}
