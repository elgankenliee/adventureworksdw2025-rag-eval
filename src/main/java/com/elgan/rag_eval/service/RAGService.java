package com.elgan.rag_eval.service;

import com.elgan.rag_eval.helper.SchemaTextConverter;
import com.elgan.rag_eval.model.SchemaDocument;
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

    public RAGService(SchemaService schemaService) {

        List<SchemaDocument> schemaDocs = schemaService.buildSchemaDocuments();

        // Print all schema documents to console
        System.out.printf("=== Schema Documents (%d tables) ===%n", schemaDocs.size());
        for (int i = 0; i < schemaDocs.size(); i++) {
            SchemaDocument sd = schemaDocs.get(i);
            System.out.printf("%n[%d] Table: %s%n", i + 1, sd.getTable());
            System.out.printf("    Columns (%d):%n", sd.getColumns().size());
            for (var col : sd.getColumns()) {
                System.out.printf("      - %s (%s)%n", col.getName(), col.getType());
            }
            if (!sd.getPrimaryKeys().isEmpty()) {
                System.out.printf("    PKs: %s%n", sd.getPrimaryKeys());
            }
            if (!sd.getForeignKeys().isEmpty()) {
                System.out.printf("    FKs:%n");
                for (var fk : sd.getForeignKeys()) {
                    System.out.printf("      - %s -> %s%n", fk.getColumn(), fk.getReferences());
                }
            }
        }
        System.out.printf("%n=== End of Schema Documents ===%n%n");

        List<Document> documents = schemaDocs.stream()
                .map(doc -> Document.from(SchemaTextConverter.toText(doc)))
                .toList();

        EmbeddingStoreIngestor ingestor =
                EmbeddingStoreIngestor.builder()
                        .embeddingModel(embeddingModel)
                        .embeddingStore(embeddingStore)
                        .build();

        ingestor.ingest(documents);

        retriever = EmbeddingStoreRetriever.from(embeddingStore, embeddingModel, 5);
    }

    public String retrieve(String query) {

        List<TextSegment> results = retriever.findRelevant(query);

        return results.stream()
                .map(TextSegment::text)
                .collect(Collectors.joining("\n"));
    }
}