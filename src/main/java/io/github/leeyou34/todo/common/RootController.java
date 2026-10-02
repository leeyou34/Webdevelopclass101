package io.github.leeyou34.todo.common;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 서버가 살아 있는지 확인하는 주소(GET /).
 */
@RestController
public class RootController {

	@GetMapping("/")
	Map<String, String> root() {
		return Map.of("service", "todo-api", "status", "ok");
	}
}
