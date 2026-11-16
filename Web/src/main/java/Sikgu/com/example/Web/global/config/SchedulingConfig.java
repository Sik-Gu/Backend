package Sikgu.com.example.Web.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 스케줄러 사용 (모집 기간 만료 자동 취소, D24) */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
