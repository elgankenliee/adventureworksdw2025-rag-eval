package com.elgan.rag_eval.service;

import com.elgan.rag_eval.model.ChatResponse;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


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
        You are an assistant that answers questions about a data warehouse.
        
        Generate a valid single-line T-SQL (Microsoft SQL Server) query only. Output must contain no line breaks, no formatting, no comments, no explanations, and no markdown.
        
        Use strictly and exclusively the tables and columns provided in the context. Do not infer, guess, or create any schema elements.
        
        Strictly follow the user’s request. Do not add extra columns, calculations, or aggregations that are not explicitly required.
        
        All non-aggregated columns in the SELECT clause must be included in the GROUP BY clause.
        
        If any required table or column is not present in the context, output exactly: INSUFFICIENT_SCHEMA.

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