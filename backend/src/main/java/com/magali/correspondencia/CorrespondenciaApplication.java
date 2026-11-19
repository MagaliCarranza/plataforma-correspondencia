package com.magali.correspondencia;

import com.magali.correspondencia.config.DemoProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(DemoProperties.class)
@EnableScheduling
public class CorrespondenciaApplication {

	public static void main(String[] args) {
		SpringApplication.run(CorrespondenciaApplication.class, args);
	}

}
