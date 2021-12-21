package com.example.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.TestRequestBodyDTO;

//실습코드 2-11. TestController.java 클래스 전체

@RestController
@RequestMapping("test") // 리소스
public class TestController {
		
	@GetMapping //testController 메소드.
	public String testController() {
		return "Hello World!";
	}
	
	//실습코드 2-12. GetMapping에 경로지정.
	@GetMapping("/testGetMapping")
	public String testControllerWithPath() {
		return "Hello World! testGetMapping ";
	}
	
	//실습코드 2-13. PathVariable를 이용한 매개변수 전달.
	//@PathVariable는 Crt + Shift + O "를 눌러서 추가하였음.
	@GetMapping("/{id}")
	public String testControllerWithPathVariables(@PathVariable(required = false) int id)
	{
		return "Hello World! ID " + id;
	}
	
	//실습코드 2-14. RequestParam를 이용한 매개변수 전달.
	//@PathVariable는 Crt + Shift + O "를 눌러서 추가하였음.
	@GetMapping("/testRequestParam")
	public String testControllerRequestParam(@RequestParam(required = false) int id ) {
		return "Hello World! ID " + id;
	}
	
	//실습코드 2-16. RequestBody 매게변수 추가.
	// /test 경로는 이미 존재하므로 /test/testRequestBody로 지정했다.
	@GetMapping("/testRequestBody")
	public String testControllerRequestBody(@RequestBody TestRequestBodyDTO testRequestBodyDTO) {
		return "Hello World! ID" + testRequestBodyDTO.getId() + "Message : " + testRequestBodyDTO.getMessage(); 
	}
}
