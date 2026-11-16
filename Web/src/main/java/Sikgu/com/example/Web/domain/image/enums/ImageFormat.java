package Sikgu.com.example.Web.domain.image.enums;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

/**
 * 허용하는 이미지 형식.
 * 확장자·Content-Type만 보면 이름만 바꾼 파일을 막을 수 없어서, 파일 앞부분 바이트(시그니처)도 확인한다.
 */
public enum ImageFormat {

    JPEG("jpg", Set.of("jpg", "jpeg"), "image/jpeg"),
    PNG("png", Set.of("png"), "image/png"),
    WEBP("webp", Set.of("webp"), "image/webp");

    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] RIFF = "RIFF".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] WEBP_MARK = "WEBP".getBytes(StandardCharsets.US_ASCII);

    /** 저장할 때 쓰는 확장자 */
    private final String storedExtension;
    private final Set<String> extensions;
    private final String contentType;

    ImageFormat(String storedExtension, Set<String> extensions, String contentType) {
        this.storedExtension = storedExtension;
        this.extensions = extensions;
        this.contentType = contentType;
    }

    public String storedExtension() {
        return storedExtension;
    }

    /** 확장자로 형식을 찾는다. 대소문자 무시 */
    public static Optional<ImageFormat> fromExtension(String extension) {
        if (extension == null) {
            return Optional.empty();
        }
        String lower = extension.toLowerCase();
        return Arrays.stream(values()).filter(f -> f.extensions.contains(lower)).findFirst();
    }

    public boolean matchesContentType(String contentType) {
        return this.contentType.equalsIgnoreCase(contentType);
    }

    /** 파일 내용이 이 형식의 시그니처로 시작하는지 */
    public boolean matchesSignature(byte[] content) {
        return switch (this) {
            case JPEG -> startsWith(content, 0, JPEG_SIGNATURE);
            case PNG -> startsWith(content, 0, PNG_SIGNATURE);
            // WEBP: "RIFF" + 4바이트 크기 + "WEBP"
            case WEBP -> startsWith(content, 0, RIFF) && startsWith(content, 8, WEBP_MARK);
        };
    }

    private static boolean startsWith(byte[] content, int offset, byte[] prefix) {
        if (content.length < offset + prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (content[offset + i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
