package br.com.fiap.gaiaops.medicao;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Testes unitarios das regras de alerta: logica pura, sem banco nem Spring. */
class RegrasDeAlertaTest {

    private static final Instant AGORA = Instant.parse("2026-09-30T12:00:00Z");

    private final RegrasDeAlerta regras = new RegrasDeAlerta();

    @Test
    @DisplayName("IQAr acima de 130 gera alerta critico - mesmo caso da praca EQP-PRA-002 na Fase 5")
    void qualidadeDoArRuimGeraAlertaCritico() {
        Optional<Document> alerta = regras.avaliar("qualidade_ar",
                Map.of("indice_iqar", 138, "mp10_ug_m3", 82), null, AGORA);

        assertThat(alerta).isPresent();
        assertThat(alerta.get().getString("nivel")).isEqualTo("critico");
        assertThat(alerta.get().get("valor_medido")).isEqualTo(138);
        assertThat(alerta.get().getBoolean("reconhecido")).isFalse();
        assertThat(alerta.get().getDate("disparado_em").toInstant()).isEqualTo(AGORA);
    }

    @Test
    void qualidadeDoArModeradaGeraAlertaMedio() {
        assertThat(regras.avaliar("qualidade_ar", Map.of("indice_iqar", 95), null, AGORA))
                .get().extracting(d -> d.getString("nivel")).isEqualTo("medio");
    }

    @Test
    void qualidadeDoArBoaNaoGeraAlerta() {
        assertThat(regras.avaliar("qualidade_ar", Map.of("indice_iqar", 63), null, AGORA)).isEmpty();
    }

    @Test
    @DisplayName("Ruido noturno usa o limite noturno informado na referencia")
    void ruidoNoturnoAcimaDoLimite() {
        Optional<Document> alerta = regras.avaliar("ruido",
                Map.of("laeq_db", 71.8, "periodo", "noturno"),
                Map.of("limite_noturno_db", 55, "limite_diurno_db", 75), AGORA);

        assertThat(alerta).isPresent();
        assertThat(alerta.get().getString("nivel")).isEqualTo("alto");
        assertThat(alerta.get().get("limite")).isEqualTo(55);
    }

    @Test
    void ruidoDiurnoDentroDoLimiteNaoGeraAlerta() {
        assertThat(regras.avaliar("ruido",
                Map.of("laeq_db", 71.8, "periodo", "diurno"),
                Map.of("limite_noturno_db", 55, "limite_diurno_db", 75), AGORA)).isEmpty();
    }

    @Test
    void perdasDeAguaAcimaDaMetaMunicipal() {
        assertThat(regras.avaliar("agua",
                Map.of("perdas_pct", 28.4), Map.of("meta_municipal_pct", 22), AGORA))
                .get().extracting(d -> d.getString("nivel")).isEqualTo("alto");
    }

    @Test
    @DisplayName("Corrego: cota de atencao gera alerta alto; cota de alerta gera critico")
    void nivelDoCorregoRespeitaAsDuasCotas() {
        Map<String, Object> cotas = Map.of("nivel_atencao_cm", 180, "nivel_alerta_cm", 240);

        assertThat(regras.avaliar("hidrologia", Map.of("nivel_cm", 218), cotas, AGORA))
                .get().extracting(d -> d.getString("nivel")).isEqualTo("alto");
        assertThat(regras.avaliar("hidrologia", Map.of("nivel_cm", 251), cotas, AGORA))
                .get().extracting(d -> d.getString("nivel")).isEqualTo("critico");
        assertThat(regras.avaliar("hidrologia", Map.of("nivel_cm", 120), cotas, AGORA)).isEmpty();
    }

    @Test
    @DisplayName("Esquema flexivel: campo ausente ou nao numerico nao derruba a regra")
    void leituraIncompletaNaoGeraAlerta() {
        assertThat(regras.avaliar("qualidade_ar", Map.of("mp10_ug_m3", 40), null, AGORA)).isEmpty();
        assertThat(regras.avaliar("qualidade_ar", Map.of("indice_iqar", "alto"), null, AGORA)).isEmpty();
        assertThat(regras.avaliar("agua", Map.of("perdas_pct", 30), null, AGORA)).isEmpty();
    }

    @Test
    void tiposSemRegraNaoGeramAlerta() {
        assertThat(regras.avaliar("energia", Map.of("kwh", 999999), null, AGORA)).isEmpty();
        assertThat(regras.avaliar("trafego", Map.of("velocidade_media_kmh", 5), null, AGORA)).isEmpty();
    }
}
