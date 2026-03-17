package com.elgan.rag_eval.service;

import com.elgan.rag_eval.config.TableDescriptionConfig;
import com.elgan.rag_eval.model.SchemaDocument;
import com.elgan.rag_eval.repository.SchemaRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SchemaService {

    private final SchemaRepository repo;
    private final TableDescriptionConfig descriptionConfig;

    public SchemaService(SchemaRepository repo, TableDescriptionConfig descriptionConfig) {
        this.repo = repo;
        this.descriptionConfig = descriptionConfig;
    }

    public List<SchemaDocument> buildSchemaDocuments() {
        var columns = repo.getColumns();
        var pks = repo.getPrimaryKeys();
        var fks = repo.getForeignKeys();

        Map<String, SchemaDocument> map = new HashMap<>();

        // Build tables + columns
        for (var row : columns) {
            String table = (String) row.get("table_name");

            map.putIfAbsent(table, new SchemaDocument());
            SchemaDocument doc = map.get(table);

            doc.setTable(table);

            SchemaDocument.Column col = new SchemaDocument.Column();
            col.setName((String) row.get("column_name"));
            col.setType((String) row.get("data_type"));

            doc.getColumns().add(col);
        }

        // Add primary keys
        for (var row : pks) {
            String table = (String) row.get("table_name");

            map.get(table)
                    .getPrimaryKeys()
                    .add((String) row.get("column_name"));
        }

        // Add foreign keys
        for (var row : fks) {
            String table = (String) row.get("table_name");

            SchemaDocument.ForeignKey fk = new SchemaDocument.ForeignKey();
            fk.setColumn((String) row.get("column_name"));
            fk.setReferences(
                    row.get("referenced_table") + "." +
                            row.get("referenced_column")
            );

            map.get(table)
                    .getForeignKeys()
                    .add(fk);
        }

        // Remove excluded tables
        Set<String> excluded = new HashSet<>(descriptionConfig.getExcludedTables());
        excluded.forEach(map::remove);

        // Merge descriptions from config
        Map<String, String> descriptions = descriptionConfig.getTableDescriptions();
        for (SchemaDocument doc : map.values()) {
            String desc = descriptions.get(doc.getTable());
            if (desc != null) {
                doc.setDescription(desc);
            }
        }

        return new ArrayList<>(map.values());
    }
}
