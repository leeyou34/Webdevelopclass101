package com.example.practice;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.demo.model.//TodoEntity; 여긴 예제 항목입니다.
//예제 2-48. @Query를 이용한 쿼리 메서드 작성..
@Repository
public interface TodoRepository extends JpaRepository<TodoEntity, String>{
	// ?1은 매서드의 매개변수의 순서 위치이다.
	@Query("select * from Todo t where t.userId = ?1")
	List<TodoEntity> findByUserId(String userId);
}

/*
*	https://docs.spring.io/spring-data/jpa/docs/current/reference/
*	html/#jpa.query-methods.query-creation
*
*	메서드 이름 작성방법과 예제는 다음 공식 사이트의 레퍼런스를 통해 확인 할 수 있다. page 131
*/