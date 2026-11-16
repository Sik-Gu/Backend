package Sikgu.com.example.Web.global.config;

import Sikgu.com.example.Web.global.infra.storage.LocalImageStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final LocalImageStorage localImageStorage;

    /** 로컬에 저장한 이미지를 /images/** 주소로 제공한다 (개발용) */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(LocalImageStorage.URL_PREFIX + "**")
                .addResourceLocations(localImageStorage.getBaseDir().toUri().toString());
    }
}
