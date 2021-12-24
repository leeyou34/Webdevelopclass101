package com.example.demo.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.TodoEntity;




// 실습코드 2-27 TodoRepository.java
@Repository
public interface TodoRepository extends JpaRepository<TodoEntity, String>{ //  <TodoEntity, Long>
	List<TodoEntity> findByUserId(String userId);
}
