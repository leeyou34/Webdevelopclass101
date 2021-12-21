package com.example.practice;

import java.util.ArrayList;
import java.util.List;

import javax.security.auth.message.callback.PrivateKeyCallback.Request;

import org.h2.util.json.JSONObject;

public class WebServiceThatSplitsToClassAndInterfaceByLayeredArchitect {
	//example 2-27. 레이어드 아키텍처를 적용해 클래스 / 인터페이스로 분리한 웹 서비스
	public List<Todo> getTodos(String userId) {
		List<Todo> todos = new ArrayList<>();
		//business logic section.
		
	
		return todos;
		}
	
	}
	
public class WebController {
		private TodoService todoService;
		
		public String getTodo(Request request) {
			//request validation
			if(request.userId == null) {
				JSONObject json = new JSONObject();
				json.put("error", "missing user id");
				return json.toString();
			}
			
			// Service layer
			List<Todo> todos = service.getTodos(request.userId);
			
			return this.getResponse(todos);

	}
}
