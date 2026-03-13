package com.elgan.rag_eval.model;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;

@Data
@Builder
@Getter
@Setter
public class ChatResponse {
    @Autowired
    private String message;
    private int requestToken;
    private int responseToken;
    private int totalToken;
}
