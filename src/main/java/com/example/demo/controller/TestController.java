package com.example.demo.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ResponseDTO;
import com.example.demo.dto.TestRequestBodyDTO;

import lombok.extern.slf4j.Slf4j;

//실습코드 2-11. TestController.java 클래스 전체
@Slf4j // 실습코드 2-30. Simple Logging Facade for Java. 
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
	@GetMapping("/testRequestBody") // 이부분은 안되는데 이유를 알아야 한다. ㅠㅠㅠ Dec 22nd
	public String testControllerRequestBody(@RequestBody TestRequestBodyDTO testRequestBodyDTO) {
		return "Hello World! ID" + testRequestBodyDTO.getId() + "Message : " + testRequestBodyDTO.getMessage(); 
	}
	
	//Dec 22nd Thomas lee started study.
	//실습코드 2-18. ResponseDTO를 반환하는 컨트롤러 메서드
	@GetMapping("/testResponseBody")
	public ResponseDTO<String> testControllerResponseBody(){
		List<String> list = new ArrayList<>();
		list.add("Hello World! I'm ResponseDTO");
		ResponseDTO<String> response = ResponseDTO.<String>builder().data(list).build();
		return response;
	}
	
	//실습코드 2-19. ResponseEntity를 반환하는 컨트롤러 메서드
	@GetMapping("/testResponseEntity")
	public ResponseEntity<?> testControllerResponseEntity() {
		List<String> list = new ArrayList<>();
		list.add("Hello World! I'm ResponseEntity. and you got 400!!");
		ResponseDTO<String> response = ResponseDTO.<String>builder().data(list).build();
		//http status를 400으로 지정
		return ResponseEntity.badRequest().body(response);
	}
}