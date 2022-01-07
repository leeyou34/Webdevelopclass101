package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
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
	
	//Jan 7th 2022, 실습코드 4-14. UserController 수정
	// Bean으로 작성해도 됨
	private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
	
	@PostMapping("/signup")
	public ResponseEntity<?> registerUser(@RequestBody UserDTO userDTO) {
		try {
			// 요청을 이용해 저장할 사용자 만들기
			UserEntity user = UserEntity.builder()
					.email(userDTO.getEmail())
					.username(userDTO.getUsername())
					.password(passwordEncoder.encode(userDTO.getPassword())) // Jan 7th 2022, 회원가입 시 유저 패스워드 암호화부분 누락되어 추가함.
					.build();
					// Thomas's personal code... below four system.out.println codes are stated to check if data is saved.."
					System.out.println("======================================================");
					System.out.println("1. ResponseEntity<?> registerUser was called ==========");
					System.out.println("2. UserEntity user was called ==========");
					System.out.println("3. UserEntity user now initiate creating user info ==========");
					System.out.println("4. UserDTO.getEmail was created ==========" + userDTO.getEmail());
					System.out.println("5. UserDTO.getUsername was created ==========" + userDTO.getUsername());
					System.out.println("6. UserDTO.getPassword was created ==========" + userDTO.getPassword());
					System.out.println("7. UserEntity user has finished the build ==========");
					System.out.println("======================================================");
					// Thomas's personal code finished
			// 서비스를 이용해 리포지터리에 사용자 저장.
			UserEntity registeredUser = userService.create(user);
					// Thomas's personal code...
					System.out.println("======================================================");
					System.out.println("1. ResponseEntity<?> registerUser was called ==========");
					System.out.println("2. UserEntity registerUser was called ==========");
					System.out.println("3. UserEntity registerUser now initiate saving user info ==========");
					System.out.println("4. UserService has finished the saving ==========");
					System.out.println("======================================================");
					// Thomas's personal code finished
			UserDTO responseUserDTO = UserDTO.builder()
					.email(registeredUser.getEmail())
					.id(registeredUser.getId())
					.username(registeredUser.getUsername())
					.build();
					// Thomas's personal code... below four system.out.println codes are stated to check if data is saved.."
					System.out.println("======================================================");
					System.out.println("1. ResponseEntity<?> registerUser was called ==========");
					System.out.println("2. UserDTO responseUserDTO was called ==========");
					System.out.println("3. UserDTO responseUserDTO now initate reading user info ==========");
					System.out.println("4. UserDTO.email(registeredUser.getEmail) was read ==========" + registeredUser.getEmail());
					System.out.println("5. UserDTO.id(registeredUser.getId) was read ==========" + registeredUser.getId());
					System.out.println("6. UserDTO.username(registeredUser.getUsername) was read ==========" + registeredUser.getUsername());
					System.out.println("7. UserDTO responseUserDTO has finished the reading ==========");
					System.out.println("======================================================");
					// Thomas's personal code finished
			return ResponseEntity.ok().body(responseUserDTO);
		} catch (Exception e) {
			// 사용자 정보는 항상 하나이므로 리스트로 만들어야 하는 ResponseDTO를 사용하지 않고 그냥 UserDTO 리턴...
			
			ResponseDTO responseDTO = ResponseDTO.builder().error(e.getMessage()).build();
					System.out.println("======================================================");
					System.out.println("1. ResponseEntity<?> registerUser was called ==========");
					System.out.println("2. ResponseDTO responseDTO was called ==========");
					System.out.println("3. There is error on ResponseDTO.builder()error==========");
					System.out.println("======================================================");
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
					System.out.println("======================================================");
					System.out.println("1. ResponseEntity<?> authenticate(@RequestBody UserDTO userDTO was called ==========");
					System.out.println("2. UserEntity user was called ==========");
					System.out.println("3. UserEntity user now initiate reading user info for autehntication ==========");
					System.out.println("4. UserDTO.getEmail was read ==========" + userDTO.getEmail());
					System.out.println("5. UserDTO.getPassword was read ==========" + userDTO.getPassword());
					System.out.println("6. UserEntity user has finished the reading==========");
					System.out.println("======================================================");

		if(user != null) {
			// 토큰 생성.  Jan 1st 2022, 실습코드 4-8. UserController의 /signin에서 토큰 생성 및 반환
			final String token = tokenProvider.create(user); // jan 3rd 2022, this code line was added.
					System.out.println("======================================================");
					System.out.println("1. ResponseEntity<?> authenticate(@RequestBody UserDTO userDTO was called ==========");
					System.out.println("2. final String token was called ==========");
					System.out.println("3. final String token now initiate create user's token. ==========");
					System.out.println("4. final String token has finished the calling TokenProvider ==========");
					System.out.println("======================================================");
			final UserDTO responseUserDTO = UserDTO.builder()
					.email(user.getEmail()) // there was typo... so this line was replaced from getUsername to getEmail https://github.com/fsoftwareengineer/todo-application/discussions/34
					.id(user.getId())
					.token(token) // jan 3rd 2022, this code line was added. 
					.build();
					System.out.println("======================================================");
					System.out.println("1. ResponseEntity<?> authenticate(@RequestBody UserDTO userDTO was called ==========");
					System.out.println("2. UserEntity user was called ==========");
					System.out.println("3. final UserDTO responseUserDTO was called===========");
					System.out.println("4. final UserDTO responseUserDTO now initiate reading ==========");
					System.out.println("5. UserDTO.email(user.getEmail()) ==========" + user.getEmail());
					System.out.println("6. UserDTO.id(user.getId()) ==========" + user.getId());
					System.out.println("7. UserDTO.token(token) ==========" + token);
					System.out.println("8. final UserDTO responseUserDTO has finished the reading==========");
					System.out.println("======================================================");
			return ResponseEntity.ok().body(responseUserDTO);
		} else {
			ResponseDTO responseDTO = ResponseDTO.builder()
					.error("Login failed.")
					.build();
					System.out.println("======================================================");
					System.out.println("1. ResponseEntity<?> authenticate(@RequestBody UserDTO userDTO was called ==========");
					System.out.println("2. ResponseDTO responseDTO was called ==========");
					System.out.println("3. There is error on ResponseDTO.builder(). the login failed. ==========");
					System.out.println("======================================================");

			
			return ResponseEntity
					.badRequest()
					.body(responseDTO);
		}				
	} 
}
