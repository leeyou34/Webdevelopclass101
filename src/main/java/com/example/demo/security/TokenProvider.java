package com.example.demo.security;

import java.nio.charset.StandardCharsets;
//import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.springframework.stereotype.Service;

import com.example.demo.model.UserEntity;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

/*===============================================================
*Dec 30th 2021, 
*Spring security consolidation (JWT 생성 및 반환 구현)
*UserService.java
* 실습코드 4-7. TokenProvider 
*================================================================
*/
	/*==================================================
	 * Jan 3rd 2022, 
	 * TokenProvider 이슈 해결 안내.
	 * https://github.com/legendmic2 유저의 글을 참고하여 응용함.
	 * 
	 * 1. Secret_Key = "...."
	 * 
	 * 	 하기 33자리 키는 Secret key를 위함이며. 
	 * 	1.1 = "#SDRTG#$%^ERG#$%GDF#$*&Dfg756DFg"; => okay to use if 하기 private static final Key key 를 사용시.
	 * 	1.2 = "NMA8JPctFuna59f5whatisthisman205"; => okay to use if 하기 private static final Key key 를 사용시.
	 *  1.3 = "NMA8JPctFuna59f5"; => if this Secret Key uses, this won't work due to HS512 issue.
	 *  1.4 = "Nn4d1MOVLZg79sfFACTIpCPKqWmpZMZQsbNrXdJJNWkRv50_l7bPLQPwhMobT4vBOG6Q3JYjhDrKFlBSaUxZOg"; 책에 있는 키 그대로 활용.
	 *  
	 *  
	 *  2. private static final Key key
	 *  
	 *  // 출처)
	 *  // https://jsonobject.tistory.com/581
	 *  // https://3edc.tistory.com/21
	 *  
	 *  하기 private static final Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
	 *  는 필요시 주석 해제 하여 쓸 수 있으나.
	 *  사용자 leeyou34는 책을 최대한 응용하여 Todo application을 구현하고자 하오니.
	 *  return Jwts.builder()에서 .signWith(Keys. blah blah, ignatureAlgorithm.HS512) 선언하여 사용함.
	 =======================================================*/

@Slf4j
@Service
public class TokenProvider {

	private static final String SECRET_KEY =  "Nn4d1MOVLZg79sfFACTIpCPKqWmpZMZQsbNrXdJJNWkRv50_l7bPLQPwhMobT4vBOG6Q3JYjhDrKFlBSaUxZOg";
		
//    private static final Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8)); // SECRET_KEY가 32byte 이하라면 compile단계에서 error 발생

	
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
		  "iat":1595733657,
		  "exp":1596597657
		  }.
		  // SECRET_KEY를 이용해 서명한 부분
		  Nn4d1MOVLZg79sfFACTIpCPKqWmpZMZQsbNrXdJJNWkRv50_l7bPLQPwhMobT4vBOG6Q3JYjhDrKFlBSaUxZOg		  
		 */
		
		// JWT Token 생성
		return Jwts.builder()
				// header에 들어갈 내용 및 서명을 하기 위한 SECRET_KEY
				//.signWith(SignatureAlgorithm.HS512, SECRET_KEY)
				.signWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS512) // thomas wrote this.
				//.signWith(key) // github.com/legendmic2 은 위의 private static final Key key에 메서드를 선언 해서 사용했다.
				// payload에 들어갈 내용
				.setSubject(userEntity.getId()) // sub
				.setIssuer("demo app") // iss
				.setIssuedAt(new Date()) // iat
				.setExpiration(expiryDate) // exp
				.compact();
		}

		public String validateAndGetUserId(String token) {
		// parseClaimsJws메서드가 Base 64로 디코딩 및 파싱.
		// 즉, 헤더와 페이로드를 setSigningKey로 넘어온 시크릿을 이용 해 서명 후, token의 서명 과 비교.
		// 위조되지 않았다면 페이로드(Claims) 리턴
		// 그 중 우리는 userId가 필요하므로 getBody를 부른다.
		Claims claims = Jwts.parserBuilder() //parser는 deprecated가 되어서 사용하려면 뒤에 builder가 필요하다....
						.setSigningKey(Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8))) // Jan 6th 2022, setSigningKey내에 SECRET_KEY를 Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8))로 변경
						.build()
						.parseClaimsJws(token)
						.getBody();
		
		return claims.getSubject();		
	}
	
}
