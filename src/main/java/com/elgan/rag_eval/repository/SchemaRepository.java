package com.elgan.rag_eval.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class SchemaRepository {

    @Autowired
    private final JdbcTemplate jdbcTemplate;

    public SchemaRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> getColumns() {
        return jdbcTemplate.queryForList("""
            SELECT 
                t.name AS table_name,
                c.name AS column_name,
                ty.name AS data_type
            FROM sys.tables t
            JOIN sys.columns c ON t.object_id = c.object_id
            JOIN sys.types ty ON c.user_type_id = ty.user_type_id
            ORDER BY t.name
        """);
    }

    public List<Map<String, Object>> getPrimaryKeys() {
        return jdbcTemplate.queryForList("""
            SELECT 
                t.name AS table_name,
                c.name AS column_name
            FROM sys.indexes i
            JOIN sys.index_columns ic 
                ON i.object_id = ic.object_id AND i.index_id = ic.index_id
            JOIN sys.columns c 
                ON ic.object_id = c.object_id AND ic.column_id = c.column_id
            JOIN sys.tables t 
                ON i.object_id = t.object_id
            WHERE i.is_primary_key = 1
        """);
    }

    public List<Map<String, Object>> getForeignKeys() {
        return jdbcTemplate.queryForList("""
            SELECT 
                tp.name AS table_name,
                cp.name AS column_name,
                tr.name AS referenced_table,
                cr.name AS referenced_column
            FROM sys.foreign_keys fk
            JOIN sys.foreign_key_columns fkc 
                ON fk.object_id = fkc.constraint_object_id
            JOIN sys.tables tp 
                ON fkc.parent_object_id = tp.object_id
            JOIN sys.columns cp 
                ON fkc.parent_object_id = cp.object_id 
                AND fkc.parent_column_id = cp.column_id
            JOIN sys.tables tr 
                ON fkc.referenced_object_id = tr.object_id
            JOIN sys.columns cr 
                ON fkc.referenced_object_id = cr.object_id 
                AND fkc.referenced_column_id = cr.column_id
        """);
    }
}
