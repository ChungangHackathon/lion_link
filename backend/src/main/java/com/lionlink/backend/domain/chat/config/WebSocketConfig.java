package com.lionlink.backend.domain.chat.config;

import com.lionlink.backend.domain.auth.config.AuthProperties;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private final StompAuthInterceptor stompAuthInterceptor;
	private final AuthProperties authProperties;
	private final JsonMapper jsonMapper;

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/ws").setAllowedOriginPatterns(authProperties.frontendUrl());
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		registry.enableSimpleBroker("/topic", "/queue");
		registry.setApplicationDestinationPrefixes("/app");
		registry.setUserDestinationPrefix("/user");
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(stompAuthInterceptor);
	}

	@Override
	public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
		messageConverters.add(new JacksonJsonMessageConverter(jsonMapper));
		return false;
	}
}
