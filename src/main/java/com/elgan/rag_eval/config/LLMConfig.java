package com.elgan.rag_eval.config;

import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class LLMConfig {

    @Value("${github.models.token}")
    private String token;

    @Bean
    public OpenAiChatModel chatModel() {
        return OpenAiChatModel.builder()
                .baseUrl("https://models.inference.ai.azure.com")
                .apiKey(token)
                .modelName("gpt-4o-mini")
                .build();
    }
}