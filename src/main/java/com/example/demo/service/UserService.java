package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.model.UserEntity;
import com.example.demo.persistence.UserRepository;

import lombok.extern.slf4j.Slf4j;

/*===============================================================
*Dec 30th 2021, 
*Authenticated Backend implementation.
*UserService.java
* 실습코드 4-3. UserService.java 
*================================================================
*/

@Slf4j
@Service
public class UserService {

	@Autowired
	private UserRepository userRepository;
	
	public UserEntity create(final UserEntity userEntity) {
		if(userEntity == null || userEntity.getEmail() == null ) {
			throw new RuntimeException("Invalid arguments");
		}
		final String email = userEntity.getEmail();
		if(userRepository.existsByEmail(email)) {
			log.warn("Email already exists {}", email );
			throw new RuntimeException("Email already exists");
		}
		
		return userRepository.save(userEntity);
	}
	
	/*==============================================================
	* Jan 7th 2022, 실습코드 4-13. 패스워드 암호화 (page 277)
	* 패스워드 암호화 부분은 스프링 시큐리티가 제공하는 BCryptPasswordEncoder의 사용을 위해 구현을 잠시 미뤄뒀음.
	* 이제 UserController와 UserService 구현하면서 스프링 시큐리티 디펜던시를 설정할 것 임.
	*
	===============================================================*/
	
	public UserEntity getByCredentials(final String email, final String password // Jan 7th 2022, 실습코드 4-13. 패스워드 암호화 시작  
			, final PasswordEncoder encoder)// this line is added.
			 {
			//return userRepository.findByEmailAndPassword(email, password); // 실습코드4-13을 위해 주석처리
			final UserEntity originalUser = userRepository.findByEmail(email);
			
			// matches 메서드를 이용해 패스워드가 같은지 확인.
			if(originalUser != null &&
					encoder.matches(password,
					originalUser.getPassword())) {
				return originalUser;
			}
			return null;
	}
}
