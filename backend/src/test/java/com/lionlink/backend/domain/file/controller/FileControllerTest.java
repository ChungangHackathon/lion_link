package com.lionlink.backend.domain.file.controller;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lionlink.backend.global.security.DevHeaderCurrentUserProvider;
import com.lionlink.backend.support.TestUsers;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FileControllerTest {

	private static final String USER_HEADER = DevHeaderCurrentUserProvider.HEADER;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private EntityManager entityManager;

	private Long me;

	@BeforeEach
	void setUp() {
		me = TestUsers.approved(entityManager, "나사자");
	}

	@Test
	void 업로드_주소를_발급한다() throws Exception {
		mockMvc.perform(post("/api/v1/files")
						.header(USER_HEADER, me)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "type": "IMAGE", "fileName": "photo.jpg", "size": 204800 }
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.uploadUrl").value(startsWith("https://test-bucket.s3.ap-northeast-2.amazonaws.com/uploads/" + me + "/")))
				.andExpect(jsonPath("$.fileUrl").value(endsWith(".jpg")))
				.andExpect(jsonPath("$.contentType").value("image/jpeg"))
				.andExpect(jsonPath("$.expiresAt").value(endsWith("+09:00")));
	}

	@Test
	void 지원하지_않는_파일이면_400() throws Exception {
		mockMvc.perform(post("/api/v1/files")
						.header(USER_HEADER, me)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "type": "IMAGE", "fileName": "virus.exe", "size": 100 }
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_FILE_TYPE"))
				.andExpect(jsonPath("$.message").value("지원하지 않는 파일 형식이에요."));
	}

	@Test
	void 필수값이_없으면_400() throws Exception {
		mockMvc.perform(post("/api/v1/files")
						.header(USER_HEADER, me)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "type": "ATTACHMENT", "size": 0 }
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors.length()").value(2));
	}

	@Test
	void 로그인하지_않으면_401() throws Exception {
		mockMvc.perform(post("/api/v1/files")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "type": "IMAGE", "fileName": "photo.jpg", "size": 100 }
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}
}
