package br.com.fiap.gaiaops.equipamento;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.fiap.gaiaops.comum.ConflitoException;
import br.com.fiap.gaiaops.comum.RecursoNaoEncontradoException;

/**
 * Testes da camada web: validacao de entrada, codigos HTTP e formato de erro.
 * O service e simulado (mock), entao estes testes nao precisam de banco.
 */
@WebMvcTest(EquipamentoController.class)
class EquipamentoControllerTest {

    private static final String NOVO_VALIDO = """
            {
              "codigo": "EQP-ESC-002",
              "nome": "EMEF Jardim das Flores",
              "tipo": "escola",
              "distrito": "Morro Alto",
              "populacao_atendida": 640,
              "atributos": { "escola": { "salas": 12 } }
            }""";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private EquipamentoService service;

    @Test
    void criaEquipamentoValidoERetorna201ComLocation() throws Exception {
        when(service.criar(any())).thenReturn(new Document("codigo", "EQP-ESC-002").append("ativo", true));

        mvc.perform(post("/api/equipamentos").contentType(MediaType.APPLICATION_JSON).content(NOVO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/equipamentos/EQP-ESC-002"))
                .andExpect(jsonPath("$.codigo").value("EQP-ESC-002"));

        // O JSON em snake_case chega ao record em camelCase.
        verify(service).criar(new NovoEquipamento("EQP-ESC-002", "EMEF Jardim das Flores", "escola",
                "Morro Alto", 640, Map.of("escola", Map.of("salas", 12))));
    }

    @Test
    void recusaCodigoForaDoPadraoETipoDesconhecido() throws Exception {
        String invalido = NOVO_VALIDO
                .replace("EQP-ESC-002", "escola-2")
                .replace("\"escola\",", "\"shopping\",");

        mvc.perform(post("/api/equipamentos").contentType(MediaType.APPLICATION_JSON).content(invalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados invalidos"))
                .andExpect(jsonPath("$.erros.codigo").value(containsString("EQP-XXX-NNN")))
                .andExpect(jsonPath("$.erros.tipo").exists());

        verify(service, never()).criar(any());
    }

    @Test
    void codigoDuplicadoRetorna409() throws Exception {
        when(service.criar(any())).thenThrow(new ConflitoException("Ja existe equipamento com o codigo EQP-ESC-002"));

        mvc.perform(post("/api/equipamentos").contentType(MediaType.APPLICATION_JSON).content(NOVO_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("EQP-ESC-002")));
    }

    @Test
    void equipamentoInexistenteRetorna404EmFormatoProblemDetail() throws Exception {
        when(service.buscar("EQP-XXX-999")).thenThrow(new RecursoNaoEncontradoException("Equipamento EQP-XXX-999 nao encontrado"));

        mvc.perform(get("/api/equipamentos/EQP-XXX-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Recurso nao encontrado"));
    }

    @Test
    void atualizacaoSemNomeRetorna400() throws Exception {
        mvc.perform(put("/api/equipamentos/EQP-ESC-001").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "tipo": "escola", "distrito": "Centro" }"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nome").exists());

        verify(service, never()).atualizar(eq("EQP-ESC-001"), any());
    }

    @Test
    void desativacaoRetorna204() throws Exception {
        mvc.perform(delete("/api/equipamentos/EQP-ESC-001"))
                .andExpect(status().isNoContent());

        verify(service).desativar("EQP-ESC-001");
    }

    @Test
    void desativarInexistenteRetorna404() throws Exception {
        doThrow(new RecursoNaoEncontradoException("Equipamento EQP-XXX-999 nao encontrado"))
                .when(service).desativar("EQP-XXX-999");

        mvc.perform(delete("/api/equipamentos/EQP-XXX-999"))
                .andExpect(status().isNotFound());
    }
}
