package Sikgu.com.example.Web.global.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import Sikgu.com.example.Web.global.apiPayload.code.ErrorStatus;
import Sikgu.com.example.Web.global.security.AuthUser;
import java.time.Clock;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {

    private final JwtProvider provider = new JwtProvider(
            new JwtProperties("test-secret-key-for-sikgu-jwt-must-be-32-bytes-or-more",
                    Duration.ofMinutes(30), Duration.ofDays(14)),
            Clock.systemUTC());
    private final AccessTokenBlacklist blacklist = mock(AccessTokenBlacklist.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(provider, blacklist);

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 유효한_Access_Token이면_인증_정보를_넣는다() throws Exception {
        MockHttpServletRequest request = requestWith("Bearer " + provider.createAccessToken(12L, "USER"));

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(new AuthUser(12L, "USER"));
        assertThat(auth.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
    }

    @Test
    void 토큰이_없으면_인증_정보_없이_넘긴다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull(); // 다음 필터로 넘어감
    }

    @Test
    void 위조된_토큰이면_401_에러를_남긴다() throws Exception {
        MockHttpServletRequest request = requestWith("Bearer not-a-jwt");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute(JwtAuthenticationFilter.ERROR_ATTRIBUTE)).isEqualTo(ErrorStatus.UNAUTHORIZED);
    }

    @Test
    void Refresh_Token으로는_인증되지_않는다() throws Exception {
        MockHttpServletRequest request = requestWith("Bearer " + provider.createRefreshToken(12L, "USER"));

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void 로그아웃한_토큰이면_AUTH4013_에러를_남긴다() throws Exception {
        String token = provider.createAccessToken(12L, "USER");
        given(blacklist.contains(provider.parse(token).tokenId())).willReturn(true);
        MockHttpServletRequest request = requestWith("Bearer " + token);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute(JwtAuthenticationFilter.ERROR_ATTRIBUTE))
                .isEqualTo(ErrorStatus.LOGGED_OUT_TOKEN);
    }

    private static MockHttpServletRequest requestWith(String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", authorization);
        return request;
    }
}
