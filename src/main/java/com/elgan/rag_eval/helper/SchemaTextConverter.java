package com.elgan.rag_eval.helper;

import com.elgan.rag_eval.model.SchemaDocument;

public class SchemaTextConverter {

    public static String toText(SchemaDocument doc) {
        StringBuilder sb = new StringBuilder();

        sb.append("Table: ").append(doc.getTable()).append("\n");

        if (doc.getDescription() != null) {
            sb.append("Description: ").append(doc.getDescription()).append("\n");
        }

        sb.append("Columns:\n");
        doc.getColumns().forEach(col ->
                sb.append("- ")
                        .append(col.getName())
                        .append(" (")
                        .append(col.getType())
                        .append(")\n")
        );

        if (!doc.getPrimaryKeys().isEmpty()) {
            sb.append("Primary Keys: ")
                    .append(String.join(", ", doc.getPrimaryKeys()))
                    .append("\n");
        }

        if (!doc.getForeignKeys().isEmpty()) {
            sb.append("Foreign Keys:\n");
            doc.getForeignKeys().forEach(fk ->
                    sb.append("- ")
                            .append(fk.getColumn())
                            .append(" -> ")
                            .append(fk.getReferences())
                            .append("\n")
            );
        }

        return sb.toString();
    }
}
