package io.github.leeyou34.todo;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * 초대 코드를 설정하면, 코드를 아는 사람만 가입할 수 있는지 확인합니다(체험판 운영 시나리오).
 */
@SpringBootTest(properties = "app.signup.invite-code=WELCOME-2026")
class InviteCodeTests {

	@Autowired
	WebApplicationContext context;

	MockMvc mvc;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
	}

	@Test
	void signupWithoutCodeIsRejected() throws Exception {
		mvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON)
				.content(TodoApiTests.signupJson("방문자", TodoApiTests.uniqueEmail(), "password123")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("초대 코드가 올바르지 않습니다."));
	}

	@Test
	void signupWithWrongCodeIsRejected() throws Exception {
		mvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON)
				.content(withCode("WRONG")))
			.andExpect(status().isBadRequest());
	}

	@Test
	void signupWithCorrectCodeSucceeds() throws Exception {
		mvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON)
				.content(withCode("WELCOME-2026")))
			.andExpect(status().isOk());
	}

	private static String withCode(String code) {
		return "{\"username\":\"방문자\",\"email\":\"" + TodoApiTests.uniqueEmail()
			+ "\",\"password\":\"password123\",\"inviteCode\":\"" + code + "\"}";
	}
}
