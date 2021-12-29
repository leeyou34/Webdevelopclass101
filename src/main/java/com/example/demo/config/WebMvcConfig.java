package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration // 스프링 빈으로 등록 
// 실습코드 3-40. Cross Origin Resource Origin 설정을 위한 WebMvcConfig
public class WebMvcConfig implements WebMvcConfigurer{
	private final long MAX_AGE_SECS = 3600;
	
	@Override
	public void addCorsMappings(CorsRegistry registry) {
		// 모든 경로에 대해
		registry.addMapping("/**")
		// Origin이 http:localhost:3000에 대해
		.allowedOrigins("http://localhost:3000")
		//Get, Post, Put, Patch, Delete, Options 메서드를 허용한다.
		.allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
		.allowedHeaders("*")
		.allowCredentials(true)
		.maxAge(MAX_AGE_SECS);
	}
}
