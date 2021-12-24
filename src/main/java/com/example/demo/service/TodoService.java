package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.TodoEntity;
import com.example.demo.persistence.TodoRepository;

import lombok.extern.slf4j.Slf4j;
@Slf4j // 실습코드 2-30. Simple Logging Facade for Java. 
@Service
public class TodoService {
	
	@Autowired // 실습코드 2-28. TodoService에서 TodoRepository 사용
	private TodoRepository repository;
	
	public String testService() {
		// TodoEntity 생성
		TodoEntity entity = TodoEntity.builder().title("My first todo item").build();
		// TodoEntity 저장
		repository.save(entity);
		// TodoEntity 검색
		TodoEntity savedEntity = repository.findById(entity.getId()).get();
		return savedEntity.getTitle();
				//"Test Service"; 이 코드는 12월 24일 현재 주석처리 
	}
}
