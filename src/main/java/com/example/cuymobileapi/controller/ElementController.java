package com.example.cuymobileapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.ArrayList;

@Tag(name = "Inventario de Red", description = "Consulta los elementos físicos y funciones de red del operador Cuy Mobile distribuidos en 14 sites del Perú")
@RestController
@RequestMapping("/api/v1/elements")
public class ElementController {

    private final MongoTemplate mongoTemplate;

    public ElementController(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Operation(
            summary = "Listar elementos de red",
            description = "Devuelve todos los servidores, switches, storage y funciones de red (AMF, SMF, UPF, etc.) distribuidos en los 14 sites de Cuy Mobile en Perú",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lista de elementos",
                            content = @Content(examples = @ExampleObject(value = """
                    [
                      {
                        "_id": "LIM-SRV-01",
                        "kind": "compute_server",
                        "name": "compute-lim-01",
                        "location": {"site": "LIM", "region": "Lima", "tier": 1},
                        "status": {"enabled": true, "maintenance_mode": false},
                        "hardware": {"cpu_cores": 64, "ram_gb": 512}
                      },
                      {
                        "_id": "LIM-AMF-01",
                        "kind": "network_function",
                        "nf_type": "AMF",
                        "name": "amf-lim-01",
                        "location": {"site": "LIM", "region": "Lima", "tier": 1},
                        "hosted_on": "LIM-SRV-01"
                      }
                    ]
                    """)))
            }
    )
    @GetMapping
    public List<Document> getElements() {
        List<Document> elements = new ArrayList<>(mongoTemplate.find(new Query(), Document.class, "network_elements"));
        List<Document> nfs = mongoTemplate.find(new Query(), Document.class, "network_functions");
        elements.addAll(nfs);
        return elements;
    }
}