package com.example.demo.model;

import javax.persistence.Entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * Author: Thomas Lee
 * Date: Dec 21st 2021
 * 2-6
 */

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data

public class TodoEntity {
	private String id; // 이 오브젝트의 아이디
	private String userId; // 이 오브젝트를 생선한 사용자의 아이디
	private String title; // Todo 타이틀 (예: 운동하기)
	private boolean done; // true - todo를 완료한 경우 (checked)

}

/*
* In this project, the model and entity implements into a class.
* Therefore, Model does two roles which are business data containing role and expressing DB table and schema. 
* It is happening because this project is small project. this model and entity name is TodoEntity and it belongs to Todo List's item
*/