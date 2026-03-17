package com.elgan.rag_eval.service;

import com.elgan.rag_eval.model.SchemaDocument;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RAGService {

    // ── Tuning knobs ─────────────────────────────────────────────────────────
    private static final int TABLE_TOP_K    = 5;   // Stage 1: top-N tables
    private static final int COLUMN_FETCH_K = 50;  // Stage 2: candidates before filter

    // ── Embedding infrastructure ──────────────────────────────────────────────
    private final EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

    private final EmbeddingStore<TextSegment> tableStore  = new InMemoryEmbeddingStore<>();
    private final EmbeddingStore<TextSegment> columnStore = new InMemoryEmbeddingStore<>();

    // ── Constructor: build both stores at startup ─────────────────────────────
    public RAGService(SchemaService schemaService) {

        List<SchemaDocument> schemaDocs = schemaService.buildSchemaDocuments();

        // Debug: print loaded schema
        System.out.printf("%n=== Schema Documents (%d tables) ===%n", schemaDocs.size());
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

        // Ingest embeddings
        int totalColumns = 0;
        for (SchemaDocument sd : schemaDocs) {
            ingestTable(sd);
            ingestColumns(sd);
            totalColumns += sd.getColumns().size();
        }
        System.out.printf("%n=== Embedded %d tables, %d columns ===%n%n",
                schemaDocs.size(), totalColumns);
    }

    // ── Stage ingestion ───────────────────────────────────────────────────────

    private void ingestTable(SchemaDocument sd) {
        StringBuilder sb = new StringBuilder();
        sb.append("Table: ").append(sd.getTable()).append("\n");

        if (sd.getDescription() != null) {
            sb.append("Description: ").append(sd.getDescription()).append("\n");
        }

        String columnKeywords = sd.getColumns().stream()
                .map(SchemaDocument.Column::getName)
                .collect(Collectors.joining(", "));
        sb.append("Columns: ").append(columnKeywords).append("\n");

        if (!sd.getPrimaryKeys().isEmpty()) {
            sb.append("Primary Keys: ")
                    .append(String.join(", ", sd.getPrimaryKeys()))
                    .append("\n");
        }

        if (!sd.getForeignKeys().isEmpty()) {
            sb.append("Foreign Keys: ")
                    .append(sd.getForeignKeys().stream()
                            .map(fk -> fk.getColumn() + " -> " + fk.getReferences())
                            .collect(Collectors.joining(", ")))
                    .append("\n");
        }

        Metadata meta = Metadata.from(Map.of("type", "table", "tableName", sd.getTable()));
        TextSegment segment = TextSegment.from(sb.toString(), meta);
        Embedding embedding = embeddingModel.embed(segment.text()).content();
        tableStore.add(embedding, segment);
    }

    private void ingestColumns(SchemaDocument sd) {
        for (SchemaDocument.Column col : sd.getColumns()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Column: ").append(col.getName())
                    .append(" (").append(col.getType()).append(")")
                    .append(" in table ").append(sd.getTable()).append("\n");

            if (sd.getDescription() != null) {
                sb.append("Table context: ").append(sd.getDescription()).append("\n");
            }

            Metadata meta = Metadata.from(Map.of(
                    "type", "column",
                    "tableName", sd.getTable(),
                    "columnName", col.getName()
            ));
            TextSegment segment = TextSegment.from(sb.toString(), meta);
            Embedding embedding = embeddingModel.embed(segment.text()).content();
            columnStore.add(embedding, segment);
        }
    }

    // ── Two-stage retrieval ───────────────────────────────────────────────────

    public String retrieve(String query) {
        Embedding queryEmbedding = embeddingModel.embed(query).content();

        // Stage 1: find the most relevant tables
        List<EmbeddingMatch<TextSegment>> tableMatches =
                tableStore.search(EmbeddingSearchRequest.builder()
                        .queryEmbedding(queryEmbedding)
                        .maxResults(TABLE_TOP_K)
                        .build()).matches();

        Set<String> selectedTables = tableMatches.stream()
                .map(m -> m.embedded().metadata().getString("tableName"))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // Stage 2: find column candidates, filter to selected tables only
        List<EmbeddingMatch<TextSegment>> columnCandidates =
                columnStore.search(EmbeddingSearchRequest.builder()
                        .queryEmbedding(queryEmbedding)
                        .maxResults(COLUMN_FETCH_K)
                        .build()).matches();

        // Group relevant columns by table (preserving table order from Stage 1)
        Map<String, List<String>> columnsByTable = new LinkedHashMap<>();
        selectedTables.forEach(t -> columnsByTable.put(t, new java.util.ArrayList<>()));

        for (EmbeddingMatch<TextSegment> match : columnCandidates) {
            String table = match.embedded().metadata().getString("tableName");
            String column = match.embedded().metadata().getString("columnName");
            if (columnsByTable.containsKey(table)) {
                columnsByTable.get(table).add(column);
            }
        }

        return buildContext(tableMatches, columnsByTable);
    }

    // ── Context formatter ─────────────────────────────────────────────────────

    private String buildContext(
            List<EmbeddingMatch<TextSegment>> tableMatches,
            Map<String, List<String>> columnsByTable) {

        StringBuilder ctx = new StringBuilder();
        ctx.append("Relevant database schema:\n\n");

        for (EmbeddingMatch<TextSegment> tableMatch : tableMatches) {
            String tableName = tableMatch.embedded().metadata().getString("tableName");

            // Table header (description line from its embedding text)
            ctx.append(tableMatch.embedded().text().strip()).append("\n");

            // Relevant columns for this table
            List<String> cols = columnsByTable.getOrDefault(tableName, List.of());
            if (!cols.isEmpty()) {
                ctx.append("Relevant Columns: ").append(String.join(", ", cols)).append("\n");
            }
            ctx.append("\n");
        }

        return ctx.toString();
    }
}