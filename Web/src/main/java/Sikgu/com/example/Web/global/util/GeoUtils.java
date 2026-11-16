package Sikgu.com.example.Web.global.util;

/**
 * 위경도 거리 계산.
 */
public final class GeoUtils {

    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * 두 지점 사이 직선 거리(km, Haversine). 소수 첫째 자리에서 반올림한다.
     * 좌표 중 하나라도 없으면 null.
     */
    public static Double distanceKm(Double lat1, Double lng1, Double lat2, Double lng2) {
        if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) {
            return null;
        }
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double km = EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return roundOneDecimal(km);
    }

    /** 두 지점 사이 직선 거리(m, 반올림 없음). 거리 구간 판단용. 좌표 중 하나라도 없으면 null */
    public static Double distanceMeters(Double lat1, Double lng1, Double lat2, Double lng2) {
        if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) {
            return null;
        }
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return EARTH_RADIUS_KM * 1000 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public static double roundOneDecimal(double value) {
        return Math.round(value * 10) / 10.0;
    }

    private GeoUtils() {
    }
}
