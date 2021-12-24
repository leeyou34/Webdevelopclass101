package com.example.demo.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/*
 * Author: Thomas Lee
 * Date: Dec 21st 2021
 * 2-8
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@Slf4j // 실습코드 2-30. Simple Logging Facade for Java. 
public class ResponseDTO<T> {
	private String error;
	private List<T> data;
}
