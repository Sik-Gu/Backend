package Sikgu.com.example.Web.domain.image.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import Sikgu.com.example.Web.domain.image.dto.response.ImageResponses.ImageUploadResponse;
import Sikgu.com.example.Web.domain.image.dto.response.ImageResponses.UploadedImage;
import Sikgu.com.example.Web.domain.image.enums.ImageType;
import Sikgu.com.example.Web.global.apiPayload.code.ErrorStatus;
import Sikgu.com.example.Web.global.apiPayload.exception.GeneralException;
import Sikgu.com.example.Web.global.infra.storage.ImageStorage;
import Sikgu.com.example.Web.global.infra.storage.ImageStorageException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class ImageServiceTest {

    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10};
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};

    /** 저장된 파일을 기억하는 가짜 저장소 */
    static class FakeStorage implements ImageStorage {
        final Map<String, byte[]> saved = new LinkedHashMap<>();
        boolean fail;

        @Override
        public String store(String path, byte[] content) {
            if (fail) {
                throw new ImageStorageException("disk full", null);
            }
            saved.put(path, content);
            return "/images/" + path;
        }
    }

    private final FakeStorage storage = new FakeStorage();
    private final ImageService service = new ImageService(storage,
            Clock.fixed(Instant.parse("2026-11-15T03:00:00Z"), ZoneId.of("Asia/Seoul")));

    @Test
    void 이미지를_용도_날짜_폴더에_UUID_이름으로_저장하고_URL을_돌려준다() {
        ImageUploadResponse response = service.upload(
                List.of(file("water.JPG", "image/jpeg", JPEG), file("receipt.png", "image/png", PNG)),
                ImageType.POST);

        assertThat(response.images()).hasSize(2);
        UploadedImage first = response.images().get(0);
        assertThat(first.url()).matches("/images/post/2026/11/15/[0-9a-f-]{36}\\.jpg");
        assertThat(first.originalName()).isEqualTo("water.JPG");
        assertThat(first.size()).isEqualTo(JPEG.length);
        assertThat(response.images().get(1).url()).endsWith(".png");
        assertThat(storage.saved).hasSize(2);
    }

    @Test
    void jpeg_확장자는_jpg로_저장한다() {
        ImageUploadResponse response = service.upload(List.of(file("a.jpeg", "image/jpeg", JPEG)), ImageType.RECEIPT);

        assertThat(response.images().get(0).url()).startsWith("/images/receipt/").endsWith(".jpg");
    }

    @Test
    void 확장자만_바꾼_파일이면_IMAGE4001이고_아무것도_저장하지_않는다() {
        MultipartFile fake = file("virus.jpg", "image/jpeg", "MZ-not-an-image".getBytes());

        assertThatThrownBy(() -> service.upload(List.of(file("ok.jpg", "image/jpeg", JPEG), fake), ImageType.POST))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus").isEqualTo(ErrorStatus.INVALID_IMAGE_FORMAT);
        assertThat(storage.saved).isEmpty();
    }

    @Test
    void 허용하지_않는_확장자면_IMAGE4001() {
        assertThatThrownBy(() -> service.upload(List.of(file("a.gif", "image/gif", JPEG)), ImageType.POST))
                .extracting("errorStatus").isEqualTo(ErrorStatus.INVALID_IMAGE_FORMAT);
    }

    @Test
    void Content_Type이_다르면_IMAGE4001() {
        assertThatThrownBy(() -> service.upload(List.of(file("a.jpg", "text/plain", JPEG)), ImageType.POST))
                .extracting("errorStatus").isEqualTo(ErrorStatus.INVALID_IMAGE_FORMAT);
    }

    @Test
    void 파일이_없으면_IMAGE4002() {
        assertThatThrownBy(() -> service.upload(null, ImageType.POST))
                .extracting("errorStatus").isEqualTo(ErrorStatus.INVALID_IMAGE_COUNT);
        assertThatThrownBy(() -> service.upload(List.of(file("a.jpg", "image/jpeg", new byte[0])), ImageType.POST))
                .extracting("errorStatus").isEqualTo(ErrorStatus.INVALID_IMAGE_COUNT);
    }

    @Test
    void 열한개면_IMAGE4002() {
        List<MultipartFile> files = new ArrayList<>(Collections.nCopies(11, file("a.jpg", "image/jpeg", JPEG)));

        assertThatThrownBy(() -> service.upload(files, ImageType.POST))
                .extracting("errorStatus").isEqualTo(ErrorStatus.INVALID_IMAGE_COUNT);
    }

    @Test
    void 십MB를_넘으면_IMAGE4131() {
        byte[] big = new byte[(int) ImageService.MAX_FILE_SIZE + 1];
        System.arraycopy(JPEG, 0, big, 0, JPEG.length);

        assertThatThrownBy(() -> service.upload(List.of(file("big.jpg", "image/jpeg", big)), ImageType.POST))
                .extracting("errorStatus").isEqualTo(ErrorStatus.IMAGE_TOO_LARGE);
    }

    @Test
    void 저장소_오류면_IMAGE5001() {
        storage.fail = true;

        assertThatThrownBy(() -> service.upload(List.of(file("a.jpg", "image/jpeg", JPEG)), ImageType.POST))
                .extracting("errorStatus").isEqualTo(ErrorStatus.IMAGE_SAVE_FAILED);
    }

    private static MockMultipartFile file(String name, String contentType, byte[] content) {
        return new MockMultipartFile("files", name, contentType, content);
    }
}
