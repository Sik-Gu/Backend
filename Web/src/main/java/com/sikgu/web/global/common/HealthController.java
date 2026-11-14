package com.sikgu.web.global.common;

import com.sikgu.web.global.exception.ErrorCode;
import com.sikgu.web.global.exception.GeneralException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {


    @GetMapping
    public ApiResponse<String> health() {
        return ApiResponse.onSuccess("ok");
    }


}