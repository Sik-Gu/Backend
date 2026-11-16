package Sikgu.com.example.Web.domain.image.service;

import Sikgu.com.example.Web.domain.image.dto.response.ImageResponses.ImageUploadResponse;
import Sikgu.com.example.Web.domain.image.dto.response.ImageResponses.UploadedImage;
import Sikgu.com.example.Web.domain.image.enums.ImageFormat;
import Sikgu.com.example.Web.domain.image.enums.ImageType;
import Sikgu.com.example.Web.global.apiPayload.code.ErrorStatus;
import Sikgu.com.example.Web.global.apiPayload.exception.GeneralException;
import Sikgu.com.example.Web.global.infra.storage.ImageStorage;
import Sikgu.com.example.Web.global.infra.storage.ImageStorageException;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 이미지 업로드 (docs/API.md 5장).
 * 모든 파일을 먼저 검사한 뒤 저장한다. 하나라도 잘못되면 아무것도 저장하지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {

    static final int MAX_FILE_COUNT = 10;
    static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10MB

    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final ImageStorage imageStorage;
    private final Clock clock;

    public ImageUploadResponse upload(List<MultipartFile> files, ImageType type) {
        List<ValidatedImage> images = validate(files);

        String directory = type.name().toLowerCase(Locale.ROOT) + "/" + LocalDate.now(clock).format(DATE_PATH);
        List<UploadedImage> uploaded = new ArrayList<>();
        for (ValidatedImage image : images) {
            // 원래 파일 이름은 쓰지 않는다: 이름 충돌, 경로 조작(../) 방지
            String path = directory + "/" + UUID.randomUUID() + "." + image.format().storedExtension();
            uploaded.add(new UploadedImage(store(path, image.content()), image.originalName(), image.content().length));
        }
        return new ImageUploadResponse(uploaded);
    }

    private List<ValidatedImage> validate(List<MultipartFile> files) {
        List<MultipartFile> nonEmpty = files == null ? List.of()
                : files.stream().filter(file -> file != null && !file.isEmpty()).toList();
        if (nonEmpty.isEmpty() || nonEmpty.size() > MAX_FILE_COUNT) {
            throw new GeneralException(ErrorStatus.INVALID_IMAGE_COUNT);
        }
        return nonEmpty.stream().map(this::validateOne).toList();
    }

    private ValidatedImage validateOne(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new GeneralException(ErrorStatus.IMAGE_TOO_LARGE);
        }

        String originalName = file.getOriginalFilename();
        ImageFormat format = ImageFormat.fromExtension(StringUtils.getFilenameExtension(originalName))
                .orElseThrow(() -> new GeneralException(ErrorStatus.INVALID_IMAGE_FORMAT));
        if (!format.matchesContentType(file.getContentType())) {
            throw new GeneralException(ErrorStatus.INVALID_IMAGE_FORMAT);
        }

        byte[] content = readBytes(file);
        if (!format.matchesSignature(content)) {
            // 확장자만 바꾼 파일 (예: exe → jpg)
            throw new GeneralException(ErrorStatus.INVALID_IMAGE_FORMAT);
        }
        return new ValidatedImage(originalName, format, content);
    }

    private static byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new GeneralException(ErrorStatus.INVALID_IMAGE_FORMAT);
        }
    }

    private String store(String path, byte[] content) {
        try {
            return imageStorage.store(path, content);
        } catch (ImageStorageException e) {
            log.error("이미지 저장 실패 path={}", path, e);
            throw new GeneralException(ErrorStatus.IMAGE_SAVE_FAILED);
        }
    }

    private record ValidatedImage(String originalName, ImageFormat format, byte[] content) {
    }
}
