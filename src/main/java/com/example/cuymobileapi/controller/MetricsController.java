package com.example.cuymobileapi.controller;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@Tag(name = "Telemetría", description = "Consulta los KPIs de rendimiento de los servidores: CPU, memoria, throughput y latencia en tiempo real")
@RestController
@RequestMapping("/api/v1/elements")
public class MetricsController {

    private final CqlSession cqlSession;

    public MetricsController(CqlSession cqlSession) {
        this.cqlSession = cqlSession;
    }

    @Operation(
            summary = "Consultar métricas de un elemento",
            description = "Devuelve el último valor registrado de un KPI (cpu_pct, mem_pct, throughput_mbps, latency_ms) para un servidor específico"
    )
    @GetMapping("/{elementId}/metrics")
    public Map<String, Object> getMetrics(
            @PathVariable String elementId,
            @RequestParam(defaultValue = "cpu_pct") String metric) {

        String latestQuery =
                "SELECT ts, value FROM core5g_metrics.latest_metrics " +
                        "WHERE element_id = ? AND metric_name = ?";

        ResultSet rs = cqlSession.execute(latestQuery, elementId, metric);
        List<Map<String, Object>> points = new ArrayList<>();
        for (Row row : rs) {
            Map<String, Object> point = new HashMap<>();
            point.put("ts", row.getInstant("ts").toString());
            point.put("value", row.getDouble("value"));
            points.add(point);
        }

        return Map.of(
                "element_id", elementId,
                "metric", metric,
                "points", points
        );
    }
}