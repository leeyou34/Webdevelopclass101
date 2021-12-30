package com.example.demo.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.springframework.stereotype.Service;

import com.example.demo.model.UserEntity;

import lombok.extern.slf4j.Slf4j;

/*===============================================================
*Dec 30th 2021, 
*Spring security consolidation (JWT 생성 및 반환 구현)
*UserService.java
* 실습코드 4-7. TokenProvider 
*================================================================
*/

@Slf4j
@Service
public class TokenProvider {
	private static final String SECRET_KEY = "NMA8JPctFuna59f5";
	
	public String create(UserEntity userEntity) {
		// 기한은 지금부터 1일로 설정
		Date expiryDate = Date.from(
				Instant.now()
				.plus(1, ChronoUnit.DAYS));
				
		/*
		 { // header
		  "alg": "HS512"
		  }.
		 { //payload
		  "sub":"40288093784915d201784916a40c0001",
		  "iss": "demo app",
		  "iat": 1595733657,
		  "exp":1596597657
		  }.
		  // SECRET_KEY를 이용해 서명한 부분
		  Nn 
		  
		 */
	}
}
