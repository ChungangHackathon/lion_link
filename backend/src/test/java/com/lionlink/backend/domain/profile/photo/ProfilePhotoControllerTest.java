package com.lionlink.backend.domain.profile.photo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.lionlink.backend.domain.file.config.StorageProperties;
import com.lionlink.backend.domain.file.service.FileStorage;
import com.lionlink.backend.global.error.GlobalExceptionHandler;
import com.lionlink.backend.support.AuthIntegrationTest;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/** PROFILE-05 프로필 사진 변경·삭제. S3는 목으로 바꿔서 올리기·지우기 호출만 확인한다. */
class ProfilePhotoControllerTest extends AuthIntegrationTest {

	private static final String PHOTO = "/api/v1/me/profile/photo";
	private static final String STUDENT = "{'lionParts':['BACKEND'],'status':'STUDENT','interests':['BACKEND']}";

	private static final byte[] PNG = withHeader(new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});
	private static final byte[] JPEG = withHeader(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0});
	private static final byte[] WEBP = withHeader(new byte[] {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'});

	@MockitoBean
	private FileStorage fileStorage;

	@Autowired
	private StorageProperties storageProperties;

	// ---------- 변경 ----------

	@Test
	void 사진을_올리면_공개_주소가_내려오고_프로필에_반영된다() throws Exception {
		Session lion = onboarded("lion");

		String body = upload(lion, "me.png", "image/png", PNG)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.photoUrl").isString())
				.andReturn().getResponse().getContentAsString();
		String photoUrl = JsonPath.read(body, "$.photoUrl");

		org.junit.jupiter.api.Assertions.assertTrue(
				photoUrl.startsWith(storageProperties.publicBaseUrl() + "/profile-photos/" + lion.userId() + "/"));
		org.junit.jupiter.api.Assertions.assertTrue(photoUrl.endsWith(".png"));

		ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
		verify(fileStorage).put(key.capture(), eq(PNG), eq("image/png"));
		org.junit.jupiter.api.Assertions.assertEquals(storageProperties.publicBaseUrl() + "/" + key.getValue(), photoUrl);

		mockMvc.perform(get("/api/v1/me/profile").header(HttpHeaders.AUTHORIZATION, bearer(lion)))
				.andExpect(jsonPath("$.photoUrl").value(photoUrl));
		mockMvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, bearer(lion)))
				.andExpect(jsonPath("$.photoUrl").value(photoUrl));
	}

	@Test
	void jpg_jpeg_webp도_올릴_수_있고_확장자는_대소문자를_가리지_않는다() throws Exception {
		Session lion = onboarded("lion");

		upload(lion, "a.jpg", "image/jpeg", JPEG).andExpect(status().isOk());
		upload(lion, "b.JPEG", "image/jpeg", JPEG).andExpect(status().isOk());
		upload(lion, "c.webp", "image/webp", WEBP).andExpect(status().isOk());

		verify(fileStorage).put(anyString(), eq(WEBP), eq("image/webp"));
	}

	@Test
	void 사진을_다시_올리면_이전_파일이_지워진다() throws Exception {
		Session lion = onboarded("lion");
		String first = JsonPath.read(upload(lion, "a.png", "image/png", PNG)
				.andReturn().getResponse().getContentAsString(), "$.photoUrl");
		String firstKey = first.substring(storageProperties.publicBaseUrl().length() + 1);

		upload(lion, "b.jpg", "image/jpeg", JPEG).andExpect(status().isOk());

		verify(fileStorage).delete(firstKey);
	}

	@Test
	void 우리_저장소가_아닌_이전_주소는_지우지_않는다() throws Exception {
		Session lion = onboarded("lion");
		setPhotoUrl(lion, "https://example.com/old.png");

		upload(lion, "a.png", "image/png", PNG).andExpect(status().isOk());

		verify(fileStorage, never()).delete(anyString());
	}

	@Test
	void 이전_파일_삭제가_실패해도_변경은_성공한다() throws Exception {
		Session lion = onboarded("lion");
		upload(lion, "a.png", "image/png", PNG).andExpect(status().isOk());
		doThrow(new RuntimeException("S3 down")).when(fileStorage).delete(anyString());

		upload(lion, "b.png", "image/png", PNG).andExpect(status().isOk());
	}

	@Test
	void 저장소에_올리지_못하면_502이고_사진은_그대로다() throws Exception {
		Session lion = onboarded("lion");
		String before = JsonPath.read(upload(lion, "a.png", "image/png", PNG)
				.andReturn().getResponse().getContentAsString(), "$.photoUrl");
		doThrow(new RuntimeException("S3 down")).when(fileStorage).put(anyString(), any(), anyString());

		upload(lion, "b.png", "image/png", PNG)
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.code").value("PHOTO_UPLOAD_FAILED"));

		mockMvc.perform(get("/api/v1/me/profile").header(HttpHeaders.AUTHORIZATION, bearer(lion)))
				.andExpect(jsonPath("$.photoUrl").value(before));
		verify(fileStorage, never()).delete(anyString());
	}

	// ---------- 검증 ----------

	@Test
	void 정확히_5MB는_되고_넘으면_400_FILE_TOO_LARGE다() throws Exception {
		Session lion = onboarded("lion");

		upload(lion, "ok.png", "image/png", sized(PNG, 5 * 1024 * 1024)).andExpect(status().isOk());
		upload(lion, "big.png", "image/png", sized(PNG, 5 * 1024 * 1024 + 1))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("FILE_TOO_LARGE"));
	}

	@Test
	void 지원하지_않는_형식은_400_UNSUPPORTED_FILE_TYPE이다() throws Exception {
		Session lion = onboarded("lion");

		upload(lion, "a.gif", "image/gif", PNG).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("UNSUPPORTED_FILE_TYPE"));
		upload(lion, "noext", "image/png", PNG).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("UNSUPPORTED_FILE_TYPE"));
		upload(lion, "a.pdf", "application/pdf", PNG).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("UNSUPPORTED_FILE_TYPE"));
		verify(fileStorage, never()).put(anyString(), any(), anyString());
	}

	@Test
	void 확장자만_바꾼_파일은_400이다() throws Exception {
		Session lion = onboarded("lion");

		upload(lion, "fake.png", "image/png", "이건 사진이 아니에요".getBytes()).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("UNSUPPORTED_FILE_TYPE"));
		upload(lion, "fake.jpg", "image/jpeg", PNG).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("UNSUPPORTED_FILE_TYPE"));
		verify(fileStorage, never()).put(anyString(), any(), anyString());
	}

	@Test
	void 빈_파일과_file_누락은_400이다() throws Exception {
		Session lion = onboarded("lion");

		upload(lion, "empty.png", "image/png", new byte[0]).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
		mockMvc.perform(multipart(HttpMethod.PUT, PHOTO).header(HttpHeaders.AUTHORIZATION, bearer(lion)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(PHOTO)
						.header(HttpHeaders.AUTHORIZATION, bearer(lion))
						.contentType("application/json").content("{}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void 업로드_한도_초과_예외는_FILE_TOO_LARGE로_바뀐다() {
		var response = new GlobalExceptionHandler().handleUploadTooLarge(new MaxUploadSizeExceededException(5L * 1024 * 1024));

		org.junit.jupiter.api.Assertions.assertEquals(400, response.getStatusCode().value());
		org.junit.jupiter.api.Assertions.assertEquals("FILE_TOO_LARGE", response.getBody().code());
	}

	// ---------- 삭제 ----------

	@Test
	void 사진을_지우면_204이고_photoUrl이_null이_되며_파일이_지워진다() throws Exception {
		Session lion = onboarded("lion");
		String photoUrl = JsonPath.read(upload(lion, "a.png", "image/png", PNG)
				.andReturn().getResponse().getContentAsString(), "$.photoUrl");

		deletePhoto(lion).andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/me/profile").header(HttpHeaders.AUTHORIZATION, bearer(lion)))
				.andExpect(jsonPath("$.photoUrl").doesNotExist());
		verify(fileStorage).delete(photoUrl.substring(storageProperties.publicBaseUrl().length() + 1));
	}

	@Test
	void 사진이_없을_때_지워도_204고_저장소는_부르지_않는다() throws Exception {
		Session lion = onboarded("lion");

		deletePhoto(lion).andExpect(status().isNoContent());

		verify(fileStorage, never()).delete(anyString());
	}

	// ---------- 접근 제어 ----------

	@Test
	void 반려_온보딩_전_토큰_없음은_막힌다() throws Exception {
		Session rejected = onboarded("rejected");
		reject(rejected.userId(), "사유");
		upload(rejected, "a.png", "image/png", PNG).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("NOT_APPROVED"));
		deletePhoto(rejected).andExpect(status().isForbidden());

		Session fresh = signup("fresh");
		upload(fresh, "a.png", "image/png", PNG).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("PROFILE_NOT_COMPLETED"));
		deletePhoto(fresh).andExpect(status().isForbidden());

		mockMvc.perform(delete(PHOTO)).andExpect(status().isUnauthorized());
		mockMvc.perform(multipart(HttpMethod.PUT, PHOTO).file(new MockMultipartFile("file", "a.png", "image/png", PNG)))
				.andExpect(status().isUnauthorized());
		verify(fileStorage, never()).put(startsWith("profile-photos/"), any(), anyString());
	}

	@Test
	void 승인된_회원도_쓸_수_있다() throws Exception {
		Session lion = onboarded("lion");
		setStatus(lion.userId(), "APPROVED");

		upload(lion, "a.png", "image/png", PNG).andExpect(status().isOk());
		deletePhoto(lion).andExpect(status().isNoContent());
	}

	// ---------- 도구 ----------

	private Session onboarded(String sub) throws Exception {
		Session session = signup(sub);
		onboard(session, STUDENT).andExpect(status().isOk());
		return session;
	}

	private ResultActions upload(Session session, String fileName, String contentType, byte[] content) throws Exception {
		return mockMvc.perform(multipart(HttpMethod.PUT, PHOTO)
				.file(new MockMultipartFile("file", fileName, contentType, content))
				.header(HttpHeaders.AUTHORIZATION, bearer(session)));
	}

	private ResultActions deletePhoto(Session session) throws Exception {
		return mockMvc.perform(delete(PHOTO).header(HttpHeaders.AUTHORIZATION, bearer(session)));
	}

	private void setPhotoUrl(Session session, String url) {
		entityManager.flush();
		entityManager.createNativeQuery("update profiles set photo_url = :url where user_id = :id")
				.setParameter("url", url).setParameter("id", session.userId()).executeUpdate();
		entityManager.clear();
	}

	/** 매직 넘버 뒤를 0으로 채워 원하는 크기로 만든다. */
	private static byte[] sized(byte[] header, int size) {
		return Arrays.copyOf(header, size);
	}

	private static byte[] withHeader(byte[] header) {
		return Arrays.copyOf(header, 32);
	}
}
