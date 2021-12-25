package com.example.demo.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ResponseDTO;
import com.example.demo.dto.TodoDTO;
import com.example.demo.model.TodoEntity;
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
	
	/*
	 * 실습코드 2-34. Todo Controller - createTodo
	 * CRUD중 Create에 해당.
	 */
	@PostMapping
	public ResponseEntity<?> createTodo(@RequestBody TodoDTO dto) {
		try {
			String temporaryUserId = "temporary-user"; // temporary user id.
		
			//	(1) TodoEntity로 변환한다.
			TodoEntity entity = TodoDTO.toEntity(dto);
			
			//	(2) id를 null로 초기화한다. 생성 당시에는 id가 없어야 함.
			entity.setId(null);
			
			//	(3) 임시 사용자 아이디를 설정해 준다. 이 부분은 4장 인증과 인가에서 수정할 예정. 
			//		지금은 인증과 인가 기능이 없으므로 한 사용자 (temporary-user)만
			//		로그인 없이 사용할 수 있는 애플리케이션임.
			entity.setUserId(temporaryUserId);
			
			//	(4) 서비스를 이용해 Todo entity를 생성한다.
			List<TodoEntity> entities = service.create(entity);
			
			//	(5) 자바 스트림을 이용해 티런된 엔티티 리스트를 TodoDTO 리스트로 반환한다.	
			List<TodoDTO> dtos = entities.stream().map(TodoDTO::new).collect(Collectors.toList());
			
			//	(6) 변환된 TodoDTO 리스트를 이용해 ResponseDTO를 초기화한다.
			ResponseDTO<TodoDTO> response = ResponseDTO.<TodoDTO>builder().data(dtos).build();
			
			//	(7) ResponseDTO를 리턴한다.
			return ResponseEntity.ok().body(response);
		}
		catch (Exception e) {
			//	(8) 혹시 예외가 있는 경우 dto 대신 error에 메세지를 넣어 리턴한다.
			String error = e.getMessage();
			ResponseDTO<TodoDTO> response = ResponseDTO.<TodoDTO>builder().error(error).build();
			return ResponseEntity.badRequest().body(response);
		}
	}
	/*
	 * 실습코드 2-37. TodoController의 retrieveTodoList 메서드. 이는 새 Get메서드이다.
	 *  CRUD 중 Read에 해당.
	 */
	@GetMapping
	public ResponseEntity<?> retrieveTodoList() { //retrieve = 검색하다.
		String temporaryUserId = "temporary-user"; // temporary user id.
		
		//	(1) 서비스 메서드의 retrieve() 메서드를 사용해 Todo 리스트를 가져온다.
		List<TodoEntity> entities = service.retrieve(temporaryUserId);
		
		//	(2) 자바 스트림을 이용해 리턴된 엔티티 리스트를 TodoDTO 리스트로 변환한다.
		List<TodoDTO> dtos = entities.stream().map(TodoDTO::new).collect(Collectors.toList());
		
		//	(3) 변환된 TodoDTO 리스트를 이용해 ResponseDTO를 초기화한다.
		ResponseDTO<TodoDTO> response = ResponseDTO.<TodoDTO>builder().data(dtos).build();
		
		//	(4) ResponseDTO를 리턴한다.
		return ResponseEntity.ok().body(response);
	}
	
	/*
	 *  실습코드 2-39. TodoController의 updateTodo 메서드
	 *  Put 메서드를 구현하고 메서드 내부는 서비스 코드를 이용해 작성한다.
	 */
	@PutMapping
	public ResponseEntity<?> updateTodo(@RequestBody TodoDTO dto) {
		String temporaryUserId = "temporary-user"; // temporary user id.
		
		//	(1) dto를 entity로 변환한다.
		TodoEntity entity = TodoDTO.toEntity(dto);
		
		//	(2) id를 temporaryUserId로 초기화한다. 여기는 4장 인증과 인가에서 수정할 예정이다.
		entity.setUserId(temporaryUserId);
		
		//	(3) 서비스를 이용해 entity를 업데이트 한다.
		List<TodoEntity> entities = service.update(entity);
		
		//	(4) 자바 스트림을 이용해 entity를 업데이트 한다.
		List<TodoDTO> dtos = entities.stream().map(TodoDTO::new).collect(Collectors.toList());
		
		//	(5) 변환된 TodoDTO 리스트를 이용해 ResponseDTO를 초기화한다.
		ResponseDTO<TodoDTO> response = ResponseDTO.<TodoDTO>builder().data(dtos).build();
		
		//	(6) ResponseDTO를 리턴한다.
		return ResponseEntity.ok().body(response);
	}
	
	/*
	 *	실습코드 2-41. TodoController의 deleteTodo 메서드
	 * 	새 delete메서드를 만들어 주고 메서드 내부는 서비스 코드를 이용해 작성한다.
	 */

	@DeleteMapping
	public ResponseEntity<?> deleteTodo(@RequestBody TodoDTO dto) {
		try {
			String temporaryUserId = "temporary-user"; // temporary user id.
			
			//	(1) TodoEntity로 변환한다.
			TodoEntity entity = TodoDTO.toEntity(dto);
			
			//	(2) 임시 사용자 아이디를 설정해 준다. 이 부분은 4장, '인증과 인가'에서 수정할 예정이다. 
			//		지금은 인증과 인가 기능이 없으므로 한 사용자(temporary-user)만 로그인 없이 사용할 수 있는 애플리케이션인 셈이다.
			entity.setUserId(temporaryUserId);
			
			//	(3) 서비스를 이용해 entity를 삭제한다.
			List<TodoEntity> entities = service.delete(entity);
			
			//	(4) 자바 스트림을 이용해 리턴된 엔티티 리스트를 TodoDTO 리스트로 변환한다.
			List<TodoDTO> dtos = entities.stream().map(TodoDTO::new).collect(Collectors.toList());
			
			//	(5) 변환된 TodoDTO 리스트를 이용해 ResponseDTO를 초기화한다.
			ResponseDTO<TodoDTO> response = ResponseDTO.<TodoDTO>builder().data(dtos).build();
			
			//	(6)	ResponseDTO를 리턴한다.
			return ResponseEntity.ok().body(response);
		}
		catch(Exception e) {
			//	(7) 혹시 예외가 있는 경우 dto 대신 error에 메시지를 넣어 리턴한다.
			String error = e.getMessage();
			ResponseDTO<TodoDTO> response = ResponseDTO.<TodoDTO>builder().error(error).build();
			return ResponseEntity.badRequest().body(response);
		}
	}
}
