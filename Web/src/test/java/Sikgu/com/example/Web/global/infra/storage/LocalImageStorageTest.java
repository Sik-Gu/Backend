package Sikgu.com.example.Web.global.infra.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalImageStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void 하위_폴더를_만들어_저장하고_images_URL을_돌려준다() throws Exception {
        LocalImageStorage storage = new LocalImageStorage(tempDir.toString());

        String url = storage.store("post/2026/11/15/a.jpg", new byte[] {1, 2, 3});

        assertThat(url).isEqualTo("/images/post/2026/11/15/a.jpg");
        assertThat(Files.readAllBytes(tempDir.resolve("post/2026/11/15/a.jpg"))).containsExactly(1, 2, 3);
    }

    @Test
    void 저장_폴더_밖으로_나가는_경로는_거부한다() {
        LocalImageStorage storage = new LocalImageStorage(tempDir.toString());

        assertThatThrownBy(() -> storage.store("../escape.jpg", new byte[] {1}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(tempDir.getParent().resolve("escape.jpg")).doesNotExist();
    }
}
