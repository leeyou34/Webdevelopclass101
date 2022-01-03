package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ResponseDTO;
import com.example.demo.dto.UserDTO;
import com.example.demo.model.UserEntity;
import com.example.demo.security.TokenProvider;
import com.example.demo.service.UserService;

import lombok.extern.slf4j.Slf4j;

/*===============================================================
*Dec 30th 2021, 
*Authenticated Backend implementation.
*
* 실습코드 4-5. UserController.java 
*================================================================
*/


@Slf4j
@RestController
@RequestMapping("/auth")
public class UserController {

	@Autowired
	private UserService userService;

	//Jan 2nd 2021, 실습코드 4-8. UserController의 /signin에서 토큰 생성 및 반환
	@Autowired
	private TokenProvider tokenProvider;
	// 토큰 선언 끝.
	
	@PostMapping("/signup")
	public ResponseEntity<?> registerUser(@RequestBody UserDTO userDTO) {
		try {
			// 요청을 이용해 저장할 사용자 만들기
			UserEntity user = UserEntity.builder()
					.email(userDTO.getEmail())
					.username(userDTO.getUsername())
					.password(userDTO.getPassword())
					.build();
			// 서비스를 이용해 리포지터리에 사용자 저장.
			UserEntity registeredUser = userService.create(user);
			UserDTO responseUserDTO = UserDTO.builder()
					.email(registeredUser.getEmail())
					.id(registeredUser.getId())
					.username(registeredUser.getUsername())
					.build();
			return ResponseEntity.ok().body(responseUserDTO);
		} catch (Exception e) {
			// 사용자 정보는 항상 하나이므로 리스트로 만들어야 하는 ResponseDTO를 사용하지 않고 그냥 UserDTO 리턴...
			
			ResponseDTO responseDTO = ResponseDTO.builder().error(e.getMessage()).build();
			return ResponseEntity
					.badRequest()
					.body(responseDTO);
		}
	}
	
	@PostMapping("/signin")
	public ResponseEntity<?> authenticate(@RequestBody UserDTO userDTO) {
		UserEntity user = userService.getByCredentials(
				userDTO.getEmail(),
				userDTO.getPassword());
		if(user != null) {
			// 토큰 생성.  Jan 1st 2022, 실습코드 4-8. UserController의 /signin에서 토큰 생성 및 반환
			final String token = tokenProvider.create(user); // jan 3rd 2022, this code line was added.
			final UserDTO responseUserDTO = UserDTO.builder()
					.email(user.getUsername())
					.id(user.getId())
					.token(token) // jan 3rd 2022, this code line was added. 
					.build();
			return ResponseEntity.ok().body(responseUserDTO);
		} else {
			ResponseDTO responseDTO = ResponseDTO.builder()
					.error("Login failed.")
					.build();
			return ResponseEntity
					.badRequest()
					.body(responseDTO);
		}				
	} 
}
