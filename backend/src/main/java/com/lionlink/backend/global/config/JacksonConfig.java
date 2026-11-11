package com.lionlink.backend.global.config;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;

@Configuration
public class JacksonConfig {

	public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

	@Bean
	public JsonMapperBuilderCustomizer offsetDateTimeCustomizer() {
		SimpleModule module = new SimpleModule()
				.addSerializer(LocalDateTime.class, new OffsetLocalDateTimeSerializer())
				.addDeserializer(LocalDateTime.class, new OffsetLocalDateTimeDeserializer());
		return builder -> builder.addModule(module);
	}

	static class OffsetLocalDateTimeSerializer extends ValueSerializer<LocalDateTime> {

		@Override
		public void serialize(LocalDateTime value, JsonGenerator gen, SerializationContext ctxt) {
			gen.writeString(value.atZone(ZONE).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
		}
	}

	static class OffsetLocalDateTimeDeserializer extends ValueDeserializer<LocalDateTime> {

		@Override
		public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) {
			String text = p.getValueAsString();
			try {
				TemporalAccessor parsed = DateTimeFormatter.ISO_DATE_TIME.parse(text);
				if (parsed.isSupported(ChronoField.OFFSET_SECONDS)) {
					return OffsetDateTime.from(parsed).atZoneSameInstant(ZONE).toLocalDateTime();
				}
				return LocalDateTime.from(parsed);
			} catch (DateTimeParseException e) {
				return (LocalDateTime) ctxt.handleWeirdStringValue(LocalDateTime.class, text, e.getMessage());
			}
		}
	}
}
