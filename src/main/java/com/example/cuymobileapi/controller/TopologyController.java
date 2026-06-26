package com.example.cuymobileapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Result;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@Tag(name = "Topología y Análisis de Impacto", description = "Consulta el grafo de dependencias de la red y analiza qué servicios se ven afectados ante la caída de un elemento")
@RestController
@RequestMapping("/api/v1/topology")
public class TopologyController {

    private final Driver neo4jDriver;

    public TopologyController(Driver neo4jDriver) {
        this.neo4jDriver = neo4jDriver;
    }

    @Operation(
            summary = "Análisis de impacto",
            description = "Dado un servidor o switch, devuelve todas las funciones de red (AMF, SMF, UPF, etc.) que quedarían fuera de servicio si ese elemento fallara",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Análisis exitoso",
                            content = @Content(examples = @ExampleObject(value = """
                    {
                      "failed_node": "LIM-SRV-01",
                      "impacted_count": 3,
                      "impact": [
                        {"nf": "LIM-AMF-01", "tipo": "AMF", "site": "LIM", "dependientes": []},
                        {"nf": "LIM-UDM-01", "tipo": "UDM", "site": "LIM", "dependientes": []},
                        {"nf": "LIM-UPF-01", "tipo": "UPF", "site": "LIM", "dependientes": []}
                      ]
                    }
                    """)))
            }
    )
    @GetMapping("/{elementId}/impact")
    public Map<String, Object> getImpact(@PathVariable String elementId) {
        String cypher = """
            MATCH (failed {id:$id})
            OPTIONAL MATCH (failed)<-[:HOSTED_ON]-(direct:NetworkFunction)
            OPTIONAL MATCH (failed)<-[:CONNECTED_TO]-(:Server)<-[:HOSTED_ON]-(viaSwitch:NetworkFunction)
            WITH [x IN collect(DISTINCT direct) + collect(DISTINCT viaSwitch) WHERE x IS NOT NULL] AS downNFs
            UNWIND downNFs AS d
            OPTIONAL MATCH (dep:NetworkFunction)-[:USES_INTERFACE|REGISTERED_IN*1..3]->(d)
            RETURN d.id AS nf, d.nf_type AS tipo, d.site AS site,
                   collect(DISTINCT dep.id) AS dependientes
            ORDER BY size(collect(DISTINCT dep.id)) DESC, nf
            """;

        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, Map.of("id", elementId));
            List<Map<String, Object>> impact = new ArrayList<>();
            while (result.hasNext()) {
                var record = result.next();
                Map<String, Object> row = new HashMap<>();
                row.put("nf", record.get("nf").asString());
                row.put("tipo", record.get("tipo").asString());
                row.put("site", record.get("site").asString());
                row.put("dependientes", record.get("dependientes").asList());
                impact.add(row);
            }
            return Map.of(
                    "failed_node", elementId,
                    "impacted_count", impact.size(),
                    "impact", impact
            );
        }
    }
}