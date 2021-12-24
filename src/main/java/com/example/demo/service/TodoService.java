package com.example.demo.service;

import java.util.List;

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
	//실습코드 2-31. create method
	public List<TodoEntity> create(final TodoEntity entity) {
		//Validations
		if(entity ==null) {
			log.warn("Entity cannot be null.");
			throw new RuntimeException("Entity cannot be null.");
		}
		if(entity.getUserId() == null) {
			log.warn("Unknown user.");
			throw new RuntimeException("Unknown user.");
		}
		
		repository.save(entity);
		
		log.info("Entity Id :  {} is saved. ", entity.getId());
		
		return repository.findByUserId(entity.getUserId());
	}
	
}
