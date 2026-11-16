package Sikgu.com.example.Web.global.infra.storage;

/**
 * 이미지 파일 저장소.
 * 개발 중엔 로컬 폴더({@link LocalImageStorage}), 운영에선 S3 등으로 바꿀 수 있게 인터페이스로 둔다.
 */
public interface ImageStorage {

    /**
     * 파일을 저장하고 접근 URL을 돌려준다.
     *
     * @param path 저장 경로 (예: post/2026/11/15/uuid.jpg)
     * @throws ImageStorageException 저장 실패
     */
    String store(String path, byte[] content);
}
