package br.com.fiap.gaiaops.equipamento;

import java.net.URI;
import java.util.List;

import org.bson.Document;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** CRUD dos equipamentos publicos (escolas, UBS, pracas, ETEs, ecopontos...). */
@RestController
@RequestMapping("/api/equipamentos")
@Tag(name = "Equipamentos", description = "Cadastro dos equipamentos publicos do municipio (pilar G)")
public class EquipamentoController {

    private final EquipamentoService service;

    public EquipamentoController(EquipamentoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista equipamentos, com filtros opcionais")
    public List<Document> listar(@RequestParam(required = false) String tipo,
                                 @RequestParam(required = false) String distrito,
                                 @RequestParam(required = false) Boolean ativo,
                                 @RequestParam(defaultValue = "100") int limite) {
        return service.listar(tipo, distrito, ativo, limite);
    }

    @GetMapping("/{codigo}")
    @Operation(summary = "Busca um equipamento pelo codigo (ex.: EQP-ESC-001)")
    public Document buscar(@PathVariable String codigo) {
        return service.buscar(codigo);
    }

    @PostMapping
    @Operation(summary = "Cadastra um equipamento")
    public ResponseEntity<Document> criar(@RequestBody @Valid NovoEquipamento novo) {
        Document criado = service.criar(novo);
        return ResponseEntity.created(URI.create("/api/equipamentos/" + novo.codigo())).body(criado);
    }

    @PutMapping("/{codigo}")
    @Operation(summary = "Atualiza um equipamento, preservando os atributos nao enviados")
    public Document atualizar(@PathVariable String codigo,
                              @RequestBody @Valid AtualizacaoEquipamento dados) {
        return service.atualizar(codigo, dados);
    }

    @DeleteMapping("/{codigo}")
    @Operation(summary = "Desativa um equipamento (exclusao logica)")
    public ResponseEntity<Void> desativar(@PathVariable String codigo) {
        service.desativar(codigo);
        return ResponseEntity.noContent().build();
    }
}
