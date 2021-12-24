package com.example.demo.dto;

import com.example.demo.model.TodoEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/*
 * Author: Thomas Lee
 * Date: Dec 21st 2021
 * 2-7
 */

@Builder
@NoArgsConstructor
@AllArgsConstructor 
@Data 
@Slf4j // 실습코드 2-30. Simple Logging Facade for Java. 
/* @Builder, this is the one of the design pattern(Refactoring Guru). 
*if a developer uses the @Builder Annotation, builder class is no longer in needed.
*/
// @NoArgsConstrucotr, this annotation implements the constructor that doesn't have parameter.(매개변수 없는 생성자)
// @AllArgsConstructor, this annotation implements the constructor that all member variable from class
// @DATA, this does implements the class member variable's Getter/ Setter method.

public class TodoDTO {
	private String id;
	private String title;
	private boolean done;

	public TodoDTO(final TodoEntity entity) {
		this.id = entity.getId();
		this.title = entity.getTitle();
		this.done = entity.isDone();
	}
}

