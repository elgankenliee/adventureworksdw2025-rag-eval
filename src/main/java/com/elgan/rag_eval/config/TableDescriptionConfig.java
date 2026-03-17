package com.elgan.rag_eval.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "")
@Getter
@Setter
public class TableDescriptionConfig {

    private Map<String, String> tableDescriptions = new HashMap<>();
    private List<String> excludedTables = new ArrayList<>();

    public Map<String, String> getTableDescriptions() {
        return tableDescriptions;
    }

    public void setTableDescriptions(Map<String, String> tableDescriptions) {
        this.tableDescriptions = tableDescriptions;
    }

    public List<String> getExcludedTables() {
        return excludedTables;
    }

    public void setExcludedTables(List<String> excludedTables) {
        this.excludedTables = excludedTables;
    }
}
