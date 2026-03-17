package com.elgan.rag_eval.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SchemaDocument {
    private String table;
    private String description;
    @Builder.Default
    private List<Column> columns = new ArrayList<>();
    @Builder.Default
    private List<String> primaryKeys = new ArrayList<>();
    @Builder.Default
    private List<ForeignKey> foreignKeys = new ArrayList<>();

    @Getter
    @Setter
    public static class Column {
        private String name;
        private String type;
    }

    @Getter
    @Setter
    public static class ForeignKey {
        private String column;
        private String references;
    }
}

