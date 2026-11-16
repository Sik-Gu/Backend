package Sikgu.com.example.Web.domain.post.enums;

/**
 * 나(집 위치) ↔ 글 올린 사람(글에 저장된 집 위치) 사이 거리 구간.
 *
 * <p>정확한 거리(m)를 내려주면 여러 위치에서 조회해 상대 집 위치를 역산할 수 있으므로,
 * 구간으로만 알려준다. 좌표는 어떤 응답에도 넣지 않는다.
 */
public enum DistanceRange {
    /** 500m 이내 */
    WITHIN_500M,
    /** 500m 초과 1km 이내 */
    WITHIN_1KM,
    /** 1km 초과 (상세 화면에서만 나올 수 있다. 목록에는 1km 이내 글만 나온다) */
    OVER_1KM;

    public static final int NEAR_METERS = 500;
    public static final int FAR_METERS = 1000;

    /** 거리(m)로 구간을 고른다. 거리를 모르면(위치 미설정) null */
    public static DistanceRange of(Double meters) {
        if (meters == null) {
            return null;
        }
        if (meters <= NEAR_METERS) {
            return WITHIN_500M;
        }
        return meters <= FAR_METERS ? WITHIN_1KM : OVER_1KM;
    }
}
