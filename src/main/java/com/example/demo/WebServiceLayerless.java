package com.example.demo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.security.auth.message.callback.PrivateKeyCallback.Request;

import org.h2.util.json.JSONArray;
import org.h2.util.json.JSONObject;

public class WebServiceLayerless {
	
	public String getTodo(Request request) {
		// 요청 검사
		if(request.userId == null) {
			JSONObject json = new JSONObject();
			json.put("error", "missing user id");
			return json.toString();
		}
		
		List<Todo> todos = new ArrayList<>();
		
		// 데이터베이스 콜
		String sqlSelectAllPersons = "Select * From Todo where User_ID = " + request.getUserId();
		String connectionUrl = "jdbc:mysql://mydb:3306/todo";
		
		try (Connection conn = DriverManager.getConnection(connectionUrl, "username", "password");
				PreparedStatement ps = conn.prepareStatement(sqlSelectAllPersons);
				ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				long id = rs.getLong("ID");
				String title = rs.getString("TITLE");
				Boolean isDone = rs.getBoolean("IS_DONE");
				
				todos.add(new Todo(id, title, isDone));
			}		
		}	catch (SQLException e) {
			//handle the exception
		}
		
		//응답생성
		JSONObject json = new JSONObject();
		JSONArray array = new JSONArray();
		
		for(Todo todo : todos) {
			JSONObject todoJson = new JSONObject();
			jsonObj.put("id", todo.getId());
			jsonObj.put("title", todo.getTitle());
			json.put("isdone", todo.isDone());
			array.put(json);
		}
		json.put("data", array);
		return json.toString();
	}



}
