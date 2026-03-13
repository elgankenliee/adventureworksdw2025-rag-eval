package com.elgan.rag_eval.model;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.retriever.EmbeddingStoreRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RAGService {

    private final EmbeddingModel embeddingModel =
            new AllMiniLmL6V2EmbeddingModel();

    private final EmbeddingStore<TextSegment> embeddingStore =
            new InMemoryEmbeddingStore<>();

    private final EmbeddingStoreRetriever retriever;

    public RAGService() {

        List<Document> documents = List.of(
                Document.from("FactInternetSales table contains sales transactions"),
                Document.from("DimCustomer contains customer demographic data"),
                Document.from("DimProduct contains product catalog data"),
                Document.from("DimDate contains calendar and date hierarchy information"),
                Document.from("Elgan likes to eat go go curry")
        );

        EmbeddingStoreIngestor ingestor =
                EmbeddingStoreIngestor.builder()
                        .embeddingModel(embeddingModel)
                        .embeddingStore(embeddingStore)
                        .build();

        ingestor.ingest(documents);

        retriever = EmbeddingStoreRetriever.from(embeddingStore, embeddingModel, 4);
    }

    public String retrieve(String query) {

        List<TextSegment> results = retriever.findRelevant(query);

        return results.stream()
                .map(TextSegment::text)
                .collect(Collectors.joining("\n"));
    }
}