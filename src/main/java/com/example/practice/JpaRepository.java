package com.example.practice;

import java.util.List;

import org.hibernate.criterion.Example;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.QueryByExampleExecutor;

import net.bytebuddy.asm.Advice.OffsetMapping.Sort;

@NoRepositoryBean
public interface JpaRepository<T, ID> extends PagingAndSortingRepository<T, ID>, QueryByExampleExecutor<T>{
	/*
	 * 2-47 JpaRepository.java 예제...
	 * 
	 * TodoRepository는 JpaRepository를 상속한다. JpaRepository는 기본적인 데이터베이스 오퍼레이션 인터페이스를 제고함.
	 * save, findById, findAll 등이 기본적으로 제공되는 인터페이스에 해다된다. 구현은 스프링데이터 JPA가 실행시 알아서 해준다.
	 * 그래서 우리가 save 메서드를 구현하려고 Insert into 와 같은 sql 쿼리를 작성 할 필요가 없다.
	 * 
	 */
	
	List<T> findAll();
	
	List<T> findAll(Sort var1);
	
	List<T> findAllById(Iterable<ID> var1);

	<S extends T> List<S> saveAll(Iterable<S> var1);
	
	void flush();
	
	<S extends T> S saveAndFlush(S var1);
	
	void deleteInBatch(Iterable<T> var1);
	
	void deleteAllInBatch(Iterable<T> var1);
	
	void deleteAllInBatch();
	
	T getOne(ID var1);
	
	<S extends T> List<S> findAll(Example<S> var1);
	
	<S extends T> List<S> findAll(Example<S> var1, Sort var2);
}
