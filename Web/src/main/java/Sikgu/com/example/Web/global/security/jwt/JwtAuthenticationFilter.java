package Sikgu.com.example.Web.global.security.jwt;

import Sikgu.com.example.Web.global.apiPayload.code.ErrorStatus;
import Sikgu.com.example.Web.global.security.AuthUser;
import Sikgu.com.example.Web.global.security.jwt.JwtProvider.JwtClaims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 요청마다 Access Token을 확인해서 인증 정보를 넣는다.
 * <p>토큰이 없거나 잘못됐으면 인증 정보를 넣지 않고 넘긴다.
 * 로그인이 필요한 API라면 SecurityConfig의 EntryPoint가 401을 응답한다.
 * 어떤 401인지 구분하려고 에러 종류를 request attribute에 남긴다.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** EntryPoint가 읽는 에러 종류 */
    public static final String ERROR_ATTRIBUTE = "jwtError";

    private final JwtProvider jwtProvider;
    private final AccessTokenBlacklist blacklist;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = BearerTokenResolver.resolve(request.getHeader(HttpHeaders.AUTHORIZATION));
        if (token != null) {
            authenticate(token, request);
        }
        chain.doFilter(request, response);
    }

    private void authenticate(String token, HttpServletRequest request) {
        JwtClaims claims;
        try {
            claims = jwtProvider.parse(token);
        } catch (JwtException | IllegalArgumentException e) {
            request.setAttribute(ERROR_ATTRIBUTE, ErrorStatus.UNAUTHORIZED);
            return;
        }

        if (!claims.accessToken()) {
            // Refresh Token으로는 API를 호출할 수 없다
            request.setAttribute(ERROR_ATTRIBUTE, ErrorStatus.UNAUTHORIZED);
            return;
        }
        if (blacklist.contains(claims.tokenId())) {
            request.setAttribute(ERROR_ATTRIBUTE, ErrorStatus.LOGGED_OUT_TOKEN);
            return;
        }

        AuthUser principal = new AuthUser(claims.userId(), claims.role());
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal, token, List.of(new SimpleGrantedAuthority("ROLE_" + claims.role())));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
