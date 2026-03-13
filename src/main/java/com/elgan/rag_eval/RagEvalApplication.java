package com.elgan.rag_eval;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class RagEvalApplication {

	public static void main(String[] args) {
		SpringApplication.run(RagEvalApplication.class, args);
		log.info("Service is running");
	}
}
