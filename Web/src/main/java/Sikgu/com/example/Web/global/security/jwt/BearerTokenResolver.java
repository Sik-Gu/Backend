package Sikgu.com.example.Web.global.security.jwt;

/**
 * {@code Authorization: Bearer {token}} 헤더에서 토큰만 꺼낸다.
 */
public final class BearerTokenResolver {

    private static final String PREFIX = "Bearer ";

    /** 헤더가 없거나 Bearer 형식이 아니면 null */
    public static String resolve(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(PREFIX)) {
            return null;
        }
        String token = authorizationHeader.substring(PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }

    private BearerTokenResolver() {
    }
}
