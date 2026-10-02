package io.github.leeyou34.todo;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;

/**
 * 회원가입 → 로그인 → 할 일 등록·조회·수정·삭제 전체 흐름과,
 * "다른 사람의 할 일은 건드릴 수 없다"는 보안 약속을 확인합니다.
 */
@SpringBootTest
class TodoApiTests {

	@Autowired
	WebApplicationContext context;

	MockMvc mvc;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
	}

	@Test
	void serverIsUp() throws Exception {
		mvc.perform(get("/")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ok"));
	}

	@Test
	void todoRequiresLogin() throws Exception {
		mvc.perform(get("/todo")).andExpect(status().isUnauthorized());
		mvc.perform(get("/todo").header("Authorization", "Bearer not-a-real-token"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void signupNeverReturnsPassword() throws Exception {
		String email = uniqueEmail();
		mvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON)
				.content(signupJson("홍길동", email, "password123")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value(email))
			.andExpect(jsonPath("$.password").doesNotExist())
			.andExpect(jsonPath("$.token").value(nullValue()));
	}

	@Test
	void duplicateEmailIsRejected() throws Exception {
		String email = uniqueEmail();
		signup(email, "password123");
		mvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON)
				.content(signupJson("다른사람", email, "password456")))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error").value("이미 가입된 이메일입니다."));
	}

	@Test
	void shortPasswordIsRejected() throws Exception {
		mvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON)
				.content(signupJson("홍길동", uniqueEmail(), "short")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").exists());
	}

	@Test
	void wrongPasswordIsRejected() throws Exception {
		String email = uniqueEmail();
		signup(email, "password123");
		mvc.perform(post("/auth/signin").contentType(MediaType.APPLICATION_JSON)
				.content(signinJson(email, "wrong-password")))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void fullTodoLifecycle() throws Exception {
		String token = signupAndSignin();

		mvc.perform(get("/todo").header("Authorization", bearer(token)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data", hasSize(0)));

		String created = mvc.perform(post("/todo").header("Authorization", bearer(token))
				.contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"보고서 초안 쓰기\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data", hasSize(1)))
			.andExpect(jsonPath("$.data[0].title").value("보고서 초안 쓰기"))
			.andExpect(jsonPath("$.data[0].done").value(false))
			.andReturn().getResponse().getContentAsString();
		String id = JsonPath.read(created, "$.data[0].id");

		mvc.perform(put("/todo").header("Authorization", bearer(token))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\":\"" + id + "\",\"title\":\"보고서 최종본 보내기\",\"done\":true}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data[0].title").value("보고서 최종본 보내기"))
			.andExpect(jsonPath("$.data[0].done").value(true));

		mvc.perform(delete("/todo").header("Authorization", bearer(token))
				.contentType(MediaType.APPLICATION_JSON).content("{\"id\":\"" + id + "\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data", hasSize(0)));
	}

	@Test
	void blankTitleIsRejected() throws Exception {
		String token = signupAndSignin();
		mvc.perform(post("/todo").header("Authorization", bearer(token))
				.contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"   \"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("할 일 내용을 입력해 주세요."));
	}

	/** 2022 버전의 핵심 취약점(소유자 미확인)이 막혔는지 확인합니다. */
	@Test
	void otherUsersCannotTouchMyTodo() throws Exception {
		String ownerToken = signupAndSignin();
		String otherToken = signupAndSignin();

		String created = mvc.perform(post("/todo").header("Authorization", bearer(ownerToken))
				.contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"내 할 일\"}"))
			.andReturn().getResponse().getContentAsString();
		String id = JsonPath.read(created, "$.data[0].id");

		mvc.perform(get("/todo").header("Authorization", bearer(otherToken)))
			.andExpect(jsonPath("$.data", hasSize(0)));

		mvc.perform(put("/todo").header("Authorization", bearer(otherToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\":\"" + id + "\",\"title\":\"바꿔치기\",\"done\":true}"))
			.andExpect(status().isNotFound());

		mvc.perform(delete("/todo").header("Authorization", bearer(otherToken))
				.contentType(MediaType.APPLICATION_JSON).content("{\"id\":\"" + id + "\"}"))
			.andExpect(status().isNotFound());

		mvc.perform(get("/todo").header("Authorization", bearer(ownerToken)))
			.andExpect(jsonPath("$.data", hasSize(1)))
			.andExpect(jsonPath("$.data[0].title").value("내 할 일"))
			.andExpect(jsonPath("$.data[0].done").value(false));
	}

	// ---------- helpers ----------

	private String signupAndSignin() throws Exception {
		String email = uniqueEmail();
		signup(email, "password123");
		String body = mvc.perform(post("/auth/signin").contentType(MediaType.APPLICATION_JSON)
				.content(signinJson(email, "password123")))
			.andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.token");
	}

	private void signup(String email, String password) throws Exception {
		mvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON)
				.content(signupJson("테스트", email, password)))
			.andExpect(status().isOk());
	}

	static String uniqueEmail() {
		return "user-" + UUID.randomUUID() + "@example.com";
	}

	static String signupJson(String username, String email, String password) {
		return "{\"username\":\"" + username + "\",\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
	}

	static String signinJson(String email, String password) {
		return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
	}

	static String bearer(String token) {
		return "Bearer " + token;
	}
}
