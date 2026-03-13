package com.elgan.rag_eval.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class DbTestController {

    private final JdbcTemplate jdbcTemplate;

    public DbTestController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/db-test")
    public List<Map<String, Object>> testDb() {
        String sql = "SELECT TOP 5 * FROM DimProduct";
        return jdbcTemplate.queryForList(sql);
    }
}
