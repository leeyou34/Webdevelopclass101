package com.example.demo.service;

import java.util.List;
import java.util.Optional;

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
/*	public List<TodoEntity> create(final TodoEntity entity) {
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
*/	
	//실스코드 2-32. 리펙토링한 create 메서드. 하기 validate 클래스는 private method로 리펙토링 한다.
	public List<TodoEntity> create(final TodoEntity entity) {
		//Validation
		validate(entity);
		
		repository.save(entity);
		
		log.info("Entity Id : {} is saved.", entity.getId());
		
		return repository.findByUserId(entity.getUserId());
		
	}
	
	//refactoring method
	private void validate(final TodoEntity entity) {
		if(entity == null ) {
			log.warn ("Entity cannot be null.");
			throw new RuntimeException("Entity cannot be null.");
		}
		if(entity.getUserId()==null) {
			log.warn("Unknown user.");
			throw new RuntimeException("Unknown user.");
		}
	}
	
	// 실습코드 2-36. TodoService의 retrieve 메서드.
	/*	새Todo 리스트 반환을 위해 findByUserId() 메서드를 사용한다.
	 * 
	 */
	public List<TodoEntity> retrieve(final String userId) {
		return repository.findByUserId(userId);
	}
	
	/*
	 *  실습코드 2-38. TodoService의 update 메서드.
	 *  repository, service, and controller would be implemented.
	 *  이 메서드는 Optional과 Lambda를 이용한다.
	 */
	public List<TodoEntity> update(final TodoEntity entity) {
		//	(1) 저장할 엔티티가 유효한지 확인한다. 이 메서드는 2.3.1 Create todo 에서 구현 되었음. (실습코드 2-31, 32)
		validate(entity);
		
		//	(2) 넘겨받은 엔티티 id를 이용해 TodoEntity를 가져온다. 존재하지 않는 엔티티는 업데이트할 수 없기 떄문이다.
		// Optional 객체는. Java.util.Optional API를 사용한다.
		final Optional<TodoEntity> original = repository.findById(entity.getId());
		
		original.ifPresent(todo -> {
			//	(3) 반환된 TodoEntity가 존재하면 값을 새 entity 값으로 덮어 씌운다.
			todo.setTitle(entity.getTitle());
			todo.setDone(entity.isDone());
			
			//	(4) 데이터 베이스에 새 값을 저장한다.
			repository.save(todo);
		});
		
		// 2.3.2 Retrieve Todo에서 만든 메서드를 이용해 사용자의 모든 Todo 리스트를 리턴한다.
		return retrieve(entity.getUserId());
	}

}
