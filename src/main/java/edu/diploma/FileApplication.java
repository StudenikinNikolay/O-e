package edu.diploma;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Optional;

@SpringBootApplication
public class FileApplication {

    @Value("${edu.diploma.security.allowed.methods}")
    private String METHODS;

    @Value("${edu.diploma.security.allowed.origins}")
    private String ORIGINS;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(
                                Optional.ofNullable(ORIGINS).map(x -> x.split(",")).orElseGet(() -> new String[0])
                        )
                        .allowedMethods(
                                Optional.ofNullable(METHODS).map(x -> x.split(",")).orElseGet(() -> new String[0])
                        )
                        .allowCredentials(true);
            }
        };
    }

    public static void main(String... args) {
        SpringApplication.run(FileApplication.class, args);
    }
}
