package Sikgu.com.example.Web.global.infra.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 서버의 로컬 폴더에 이미지를 저장한다 (개발용).
 * 저장한 파일은 {@code /images/**} 주소로 제공된다 (WebMvcConfig).
 */
@Component
public class LocalImageStorage implements ImageStorage {

    public static final String URL_PREFIX = "/images/";

    private final Path baseDir;

    public LocalImageStorage(@Value("${image.upload-dir:uploads}") String uploadDir) {
        this.baseDir = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    public Path getBaseDir() {
        return baseDir;
    }

    @Override
    public String store(String path, byte[] content) {
        Path target = baseDir.resolve(path).normalize();
        if (!target.startsWith(baseDir)) {
            // "../" 같은 경로로 저장 폴더 밖에 쓰는 것을 막는다
            throw new IllegalArgumentException("저장 폴더 밖의 경로입니다: " + path);
        }
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException e) {
            throw new ImageStorageException("이미지 저장 실패: " + path, e);
        }
        return URL_PREFIX + path;
    }
}
