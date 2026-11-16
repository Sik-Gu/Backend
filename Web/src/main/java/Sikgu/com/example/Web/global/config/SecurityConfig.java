package Sikgu.com.example.Web.global.config;

import Sikgu.com.example.Web.global.apiPayload.code.ErrorStatus;
import Sikgu.com.example.Web.global.security.jwt.AccessTokenBlacklist;
import Sikgu.com.example.Web.global.security.jwt.JwtAuthenticationFilter;
import Sikgu.com.example.Web.global.security.jwt.JwtProperties;
import Sikgu.com.example.Web.global.security.jwt.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    /** 토큰 없이 호출할 수 있는 API (docs/API.md 권한 "공개") */
    private static final String[] PUBLIC_URLS = {
            "/api/auth/email/send",
            "/api/auth/email/verify",
            "/api/auth/signup",
            "/api/auth/login",
            "/api/auth/reissue",
            "/error"
    };

    /** 로그인 없이 조회할 수 있는 정적 파일. <img> 태그로 바로 띄워야 해서 공개 */
    private static final String[] PUBLIC_GET_URLS = {
            "/images/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtProvider jwtProvider,
                                                   AccessTokenBlacklist blacklist) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET_URLS).permitAll()
                        .anyRequest().authenticated())
                // 필터를 빈으로 등록하면 서블릿 필터로도 한 번 더 등록되므로 여기서 직접 만든다
                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider, blacklist),
                        UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint((request, response, e) ->
                                writeError(response, resolveAuthError(request)))
                        .accessDeniedHandler((request, response, e) ->
                                writeError(response, ErrorStatus.FORBIDDEN)));
        return http.build();
    }

    /** 비밀번호는 BCrypt로 저장한다 (docs/DB.md D7). salt는 해시 문자열 안에 포함된다. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** 현재 시각. 테스트에서 시간을 고정할 수 있게 빈으로 둔다. */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    /** JWT 필터가 남긴 에러 종류 (로그아웃한 토큰 등). 없으면 일반 401 */
    private static ErrorStatus resolveAuthError(HttpServletRequest request) {
        Object error = request.getAttribute(JwtAuthenticationFilter.ERROR_ATTRIBUTE);
        return error instanceof ErrorStatus status ? status : ErrorStatus.UNAUTHORIZED;
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
