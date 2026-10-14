package br.com.fiap.gaiaops.programa;

import java.util.List;

import org.bson.Document;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/programas-sociais")
@Tag(name = "Programas sociais", description = "Politicas publicas e beneficiarios (pilar S)")
public class ProgramaSocialController {

    private final ProgramaSocialService service;

    public ProgramaSocialController(ProgramaSocialService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista programas sociais, com filtro opcional por eixo")
    public List<Document> listar(@RequestParam(required = false) String eixo) {
        return service.listar(eixo);
    }

    @GetMapping("/{codigo}")
    @Operation(summary = "Busca um programa pelo codigo (ex.: PRG-TR-001)")
    public Document buscar(@PathVariable String codigo) {
        return service.buscar(codigo);
    }
}
