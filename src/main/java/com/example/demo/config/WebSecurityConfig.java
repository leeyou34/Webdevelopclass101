package com.example.demo.config;

import org.springframework.beans.factory.annotation.Autowired;

import com.example.demo.security.JwtAuthenticationFilter;

import lombok.extern.slf4j.Slf4j;

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
	}
}
