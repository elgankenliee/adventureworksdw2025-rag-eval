package com.elgan.rag_eval.service;

import com.elgan.rag_eval.model.ChatResponse;
import com.elgan.rag_eval.model.RAGService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


@Service
public class ChatService {
    @Autowired
    private final OpenAiChatModel model;
    private final RAGService ragService;

    public ChatService(OpenAiChatModel model, RAGService ragService) {
        this.model = model;
        this.ragService = ragService;
    }

    public ChatResponse chat(String message) {

        String context = ragService.retrieve(message);

        String prompt = """
        You are an assistant that answers questions about data warehouse.

        Context:
        %s

        Question:
        %s
        """.formatted(context, message);


        UserMessage userMessage = UserMessage.from(prompt);

        Response<AiMessage> response = model.generate(List.of(userMessage));

        String answer = response.content().text();

        var tokens = response.tokenUsage();
        int inputTokens = tokens.inputTokenCount();
        int outputTokens = tokens.outputTokenCount();
        int totalTokens = tokens.totalTokenCount();

        return ChatResponse.builder()
                .message(answer)
                .requestToken(inputTokens)
                .responseToken(outputTokens)
                .totalToken(totalTokens)
                .build();
    }

    public String retrieveTables(String message) {
        String retrievedResult = ragService.retrieve(message);
        return retrievedResult;
    }

}