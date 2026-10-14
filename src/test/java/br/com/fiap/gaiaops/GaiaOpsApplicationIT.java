package br.com.fiap.gaiaops;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Teste de integracao ponta a ponta: aplicacao completa + MongoDB real.
 *
 * O Testcontainers sobe um container mongo:8.0 descartavel, a carga inicial
 * popula a base com os dados da Fase 5 e os cenarios abaixo exercitam a API
 * como um cliente faria. Os metodos rodam em ordem porque contam uma historia
 * (cadastra, altera, desativa e entao tenta usar o equipamento desativado).
 *
 * Exige Docker. Sem Docker (ex.: maquina de desenvolvimento sem Docker Desktop)
 * a classe e ignorada; no pipeline do GitHub Actions ela sempre executa.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GaiaOpsApplicationIT {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:8.0");

    @Autowired
    private MockMvc mvc;

    @Test
    @Order(1)
    @DisplayName("Carga inicial popula as 5 collections com os 100 documentos da Fase 5")
    void cargaInicialPopulaAsCincoCollections() throws Exception {
        mvc.perform(get("/api/equipamentos")).andExpect(jsonPath("$", hasSize(12)));
        mvc.perform(get("/api/medicoes").param("limite", "500")).andExpect(jsonPath("$", hasSize(53)));
        mvc.perform(get("/api/residuos")).andExpect(jsonPath("$", hasSize(12)));
        mvc.perform(get("/api/programas-sociais")).andExpect(jsonPath("$", hasSize(11)));
        mvc.perform(get("/api/conformidade")).andExpect(jsonPath("$", hasSize(12)));
    }

    @Test
    @Order(2)
    @DisplayName("Painel ESG agrega corretamente os dados da base")
    void painelEsgCalculadoSobreABase() throws Exception {
        mvc.perform(get("/api/indicadores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ambiental.medicoes").value(53))
                .andExpect(jsonPath("$.ambiental.alertas_abertos").value(6))
                .andExpect(jsonPath("$.ambiental.residuos_coletados_kg").value(83702.0))
                .andExpect(jsonPath("$.ambiental.taxa_desvio_de_aterro_pct").value(49.0))
                .andExpect(jsonPath("$.social.programas_ativos").value(11))
                .andExpect(jsonPath("$.social.beneficiarios_acompanhados").value(25))
                .andExpect(jsonPath("$.social.execucao_orcamentaria_pct").value(58.5))
                .andExpect(jsonPath("$.governanca.equipamentos_total").value(12))
                .andExpect(jsonPath("$.governanca.licencas_por_vencer").isNumber())
                .andExpect(jsonPath("$.governanca.manifestacoes_ouvidoria").value(3));
    }

    @Test
    @Order(3)
    @DisplayName("Ciclo completo de um equipamento: cria, consulta, atualiza e desativa")
    void cicloCompletoDeUmEquipamento() throws Exception {
        mvc.perform(post("/api/equipamentos").contentType(MediaType.APPLICATION_JSON).content("""
                        {
                          "codigo": "EQP-ESC-900",
                          "nome": "EMEF Teste de Integracao",
                          "tipo": "escola",
                          "distrito": "Morro Alto",
                          "populacao_atendida": 300,
                          "atributos": { "escola": { "salas": 8 } }
                        }"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$._id").value(matchesPattern("[0-9a-f]{24}")))
                .andExpect(jsonPath("$.ativo").value(true));

        // Codigo repetido: conflito
        mvc.perform(post("/api/equipamentos").contentType(MediaType.APPLICATION_JSON).content("""
                        { "codigo": "EQP-ESC-900", "nome": "Outra", "tipo": "escola", "distrito": "Centro" }"""))
                .andExpect(status().isConflict());

        // A atualizacao com $set nao apaga o atributo "escola", que nao foi reenviado
        mvc.perform(put("/api/equipamentos/EQP-ESC-900").contentType(MediaType.APPLICATION_JSON).content("""
                        {
                          "nome": "EMEF Teste de Integracao II",
                          "tipo": "escola",
                          "distrito": "Morro Alto",
                          "atributos": { "metas_esg": { "energia_kwh_mes": 4000 } }
                        }"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("EMEF Teste de Integracao II"))
                .andExpect(jsonPath("$.escola.salas").value(8))
                .andExpect(jsonPath("$.metas_esg.energia_kwh_mes").value(4000));

        mvc.perform(delete("/api/equipamentos/EQP-ESC-900")).andExpect(status().isNoContent());

        // Exclusao logica: o documento continua na base, so que inativo
        mvc.perform(get("/api/equipamentos/EQP-ESC-900"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));
    }

    @Test
    @Order(4)
    @DisplayName("Medicao com IQAr alto e gravada com alerta critico e aparece no painel de alertas")
    void medicaoComIqarAltoDisparaAlerta() throws Exception {
        mvc.perform(post("/api/medicoes").contentType(MediaType.APPLICATION_JSON).content("""
                        {
                          "equipamento_id": "EQP-PRA-002",
                          "tipo": "qualidade_ar",
                          "sensor": { "id": "SEN-AR-004" },
                          "leitura": { "mp10_ug_m3": 95, "indice_iqar": 151 }
                        }"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.alerta.nivel").value("critico"))
                .andExpect(jsonPath("$.alerta.reconhecido").value(false));

        mvc.perform(get("/api/medicoes/alertas").param("nivel", "critico"))
                .andExpect(jsonPath("$[*].leitura.indice_iqar", hasItem(151)))
                .andExpect(jsonPath("$[*].alerta.nivel", everyItem(is("critico"))));
    }

    @Test
    @Order(5)
    @DisplayName("Equipamento desativado nao recebe medicoes (422)")
    void equipamentoDesativadoNaoRecebeMedicoes() throws Exception {
        mvc.perform(post("/api/medicoes").contentType(MediaType.APPLICATION_JSON).content("""
                        { "equipamento_id": "EQP-ESC-900", "tipo": "energia", "leitura": { "kwh": 100 } }"""))
                .andExpect(status().isUnprocessableEntity());

        mvc.perform(post("/api/medicoes").contentType(MediaType.APPLICATION_JSON).content("""
                        { "equipamento_id": "EQP-XYZ-999", "tipo": "energia", "leitura": { "kwh": 100 } }"""))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(6)
    @DisplayName("Readiness UP: e o mesmo endpoint usado pelo HEALTHCHECK do Docker e pelo smoke test")
    void readinessIndicaAplicacaoPronta() throws Exception {
        mvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
