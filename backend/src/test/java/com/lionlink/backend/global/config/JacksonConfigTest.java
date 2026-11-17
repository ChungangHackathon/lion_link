package com.lionlink.backend.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
class JacksonConfigTest {

	record TimeHolder(LocalDateTime time) {
	}

	@Autowired
	private JsonMapper jsonMapper;

	@Test
	void 시간은_서울_오프셋을_붙여_응답한다() {
		String json = jsonMapper.writeValueAsString(new TimeHolder(LocalDateTime.of(2026, 11, 8, 10, 0)));

		assertThat(json).isEqualTo("{\"time\":\"2026-11-08T10:00:00+09:00\"}");
	}

	@Test
	void 오프셋이_있는_시간은_서울_시간으로_바꿔_읽는다() {
		TimeHolder holder = jsonMapper.readValue("{\"time\":\"2026-11-08T01:00:00Z\"}", TimeHolder.class);

		assertThat(holder.time()).isEqualTo(LocalDateTime.of(2026, 11, 8, 10, 0));
	}

	@Test
	void 오프셋이_없는_시간은_그대로_읽는다() {
		TimeHolder holder = jsonMapper.readValue("{\"time\":\"2026-11-08T10:00:00\"}", TimeHolder.class);

		assertThat(holder.time()).isEqualTo(LocalDateTime.of(2026, 11, 8, 10, 0));
	}
}
