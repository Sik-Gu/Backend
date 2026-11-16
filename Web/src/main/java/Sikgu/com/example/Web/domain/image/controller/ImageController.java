package Sikgu.com.example.Web.domain.image.controller;

import Sikgu.com.example.Web.domain.image.dto.response.ImageResponses.ImageUploadResponse;
import Sikgu.com.example.Web.domain.image.enums.ImageType;
import Sikgu.com.example.Web.domain.image.service.ImageService;
import Sikgu.com.example.Web.global.apiPayload.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 이미지 업로드 (docs/API.md 5장).
 * 이미지를 먼저 올리고, 받은 URL을 공구 등록·영수증·문의·신고·채팅 API에 넣는다.
 */
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    /** 이미지 업로드 (로그인 필요). form-data: files(1~10개), type */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ImageUploadResponse> upload(
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @RequestParam("type") ImageType type) {
        return ApiResponse.onSuccess(imageService.upload(files, type));
    }
}
