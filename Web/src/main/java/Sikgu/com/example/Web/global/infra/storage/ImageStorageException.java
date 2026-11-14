package Sikgu.com.example.Web.global.infra.storage;

/** 이미지 저장 실패 */
public class ImageStorageException extends RuntimeException {

    public ImageStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
