package Sikgu.com.example.Web.global.config;

import Sikgu.com.example.Web.global.apiPayload.code.ErrorStatus;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    /** 토큰 없이 호출할 수 있는 API (docs/API.md 권한 "공개") */
    private static final String[] PUBLIC_URLS = {
            "/api/auth/email/send",
            "/api/auth/email/verify",
            "/api/auth/check-id",
            "/api/auth/signup",
            "/api/auth/login",
            "/api/auth/reissue",
            "/api/locations/**",
            "/error"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint((request, response, e) ->
                                writeError(response, ErrorStatus.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, e) ->
                                writeError(response, ErrorStatus.FORBIDDEN)));
        // TODO: 로그인 기능 구현 시 JWT 인증 필터 추가
        return http.build();
    }

    /** 비밀번호는 BCrypt로 저장한다 (docs/DB.md D7). salt는 해시 문자열 안에 포함된다. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Security 필터 단계의 401/403도 공통 응답 형식으로 내려준다. */
    private static void writeError(HttpServletResponse response, ErrorStatus status) throws IOException {
        response.setStatus(status.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("""
                {"isSuccess":false,"code":"%s","message":"%s","result":null}"""
                .formatted(status.getCode(), status.getMessage()));
    }
}
