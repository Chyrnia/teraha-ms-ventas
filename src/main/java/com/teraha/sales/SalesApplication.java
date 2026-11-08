package com.teraha.sales;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.restclient.RestTemplateBuilder;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.web.client.RestTemplate;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EntityScan(basePackages = "com.teraha.commons.entities")
public class SalesApplication {

	public static void main(String[] args) {
		SpringApplication.run(SalesApplication.class, args);
	}

	@Bean
	@LoadBalanced
	public RestTemplate restTemplate(RestTemplateBuilder builder){
		return builder.additionalInterceptors((request, body, execution) -> {
			Authentication authentication = 
				SecurityContextHolder.getContext().getAuthentication();
			if(authentication instanceof JwtAuthenticationToken jwtAuth){
				request.getHeaders().setBearerAuth(
						jwtAuth.getToken().getTokenValue()
						);
			}
			return execution.execute(request, body);
		}).build();
	}

}
