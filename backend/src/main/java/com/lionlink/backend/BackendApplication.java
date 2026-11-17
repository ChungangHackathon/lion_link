package com.lionlink.backend;

import com.lionlink.backend.global.config.JacksonConfig;
import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone(JacksonConfig.ZONE));
		SpringApplication.run(BackendApplication.class, args);
	}

}
