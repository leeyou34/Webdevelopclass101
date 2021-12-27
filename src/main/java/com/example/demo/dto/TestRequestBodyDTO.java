package com.example.demo.dto;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
// 2-15. RequestBodyDTO 실습을 위한 TestRequestBodyDTO
@Data
@Slf4j // 실습코드 2-30. Simple Logging Facade for Java. 
public class TestRequestBodyDTO {
	private int id;
	private String message;
}