package com.example.demo.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ResponseDTO;
import com.example.demo.service.TodoService;

import lombok.extern.slf4j.Slf4j;
// 실습코드 2-21. 12.23.2021
@Slf4j // 실습코드 2-30. Simple Logging Facade for Java. 
@RestController
@RequestMapping("todo")
public class TodoController {
	//TestTodo 메서드 작성하기 
	//실습코드 2-22. TodoController에서 TodoService사용
	@Autowired
	private TodoService service;
	
	@GetMapping("/test")
	public ResponseEntity<?> testTodo() {
		String str = service.testService(); //테스트 서비스 사용
		List<String> list =new ArrayList<>();
		list.add(str);
		ResponseDTO<String> response = ResponseDTO.<String>builder().data(list).build();
		return ResponseEntity.ok().body(response);
	}
}