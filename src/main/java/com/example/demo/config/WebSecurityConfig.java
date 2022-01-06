package com.example.demo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.web.filter.CorsFilter;

import com.example.demo.security.JwtAuthenticationFilter;

import lombok.extern.slf4j.Slf4j;

/*==============================================================
 * Jan 6rd 2022, 실습코드 4-11. WebSecurityConfig
 * author: thomas lee
 * WebSecurityConfig내에 발생한 오류들은 buildGradle에서 security 버젼을 잡아주니
 * import시 문제없이 각자 위치를 잘 잡아 오류를 잡음.
 ==============================================================*/


@EnableWebSecurity
@Slf4j
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
	
	@Autowired
	private JwtAuthenticationFilter jwtAuthenticationFilter;
	
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		//http security builder
		http.cors() // WebMvcConfig에서 이미 설정했으므로 기본 cors 설정
		.and()
		.csrf() // csrf는 현재 사용하지 않음으로 disable.
			.disable()
		.httpBasic() // token을 사용하지 않으므로 disable
			.disable()
		.sessionManagement() // session 기반이 아님을 선언
			.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
		.and()
		.authorizeRequests() // /와 /auth/** 경로는 인증 안해도 됨.
			.antMatchers("/", "/auth/**", "/error").permitAll() // Jan 6th 2022, "/error" was added since this springboot version is 2.6.2 veriosn. i used the reference from https://github.com/spring-projects/spring-boot/issues/28953 
		.anyRequest() // /와 /auth/** 이외의 모든 경로는 인증 해야 됨.
			.authenticated();
		// filter 등록
		// 매 요청마다
		// CorsFilter 실행한 후에
		// jwtAuthenticationFilter 실행한다.
		http.addFilterAfter(
				jwtAuthenticationFilter,
				CorsFilter.class
		);
	}
}
