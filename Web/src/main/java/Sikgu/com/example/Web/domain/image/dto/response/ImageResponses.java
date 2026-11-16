package Sikgu.com.example.Web.domain.image.dto.response;

import java.util.List;

/**
 * 이미지 API 응답 DTO 모음.
 */
public final class ImageResponses {

    /** POST /api/images */
    public record ImageUploadResponse(List<UploadedImage> images) {
    }

    /**
     * @param url          다른 API에 넣을 이미지 주소
     * @param originalName 사용자가 올린 원래 파일 이름
     * @param size         바이트 단위 크기
     */
    public record UploadedImage(String url, String originalName, long size) {
    }

    private ImageResponses() {
    }
}
