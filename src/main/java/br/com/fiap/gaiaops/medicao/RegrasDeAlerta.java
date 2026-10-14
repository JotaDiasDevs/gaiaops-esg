package br.com.fiap.gaiaops.medicao;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import org.bson.Document;
import org.springframework.stereotype.Component;

/**
 * Regras de alerta automatico aplicadas a cada medicao recebida.
 *
 * Reproduzem os alertas que ja existem na base da Fase 5 (mesmos niveis e
 * mesmos textos de regra). Sao funcoes puras - nao acessam o banco - e por
 * isso sao cobertas por testes unitarios simples.
 *
 * Tipos sem regra definida (energia, trafego) sao gravados sem alerta.
 */
@Component
public class RegrasDeAlerta {

    /** IQAr acima de 130: qualidade do ar ruim (CONAMA 491/2018). */
    static final double IQAR_CRITICO = 130;
    /** IQAr acima de 80: qualidade do ar moderada. */
    static final double IQAR_ATENCAO = 80;

    public Optional<Document> avaliar(String tipo, Map<String, Object> leitura,
                                      Map<String, Object> referencia, Instant momento) {
        Map<String, Object> ref = referencia == null ? Map.of() : referencia;

        Optional<Document> alerta = switch (tipo) {
            case "qualidade_ar" -> qualidadeDoAr(leitura);
            case "ruido" -> ruido(leitura, ref);
            case "agua" -> perdasDeAgua(leitura, ref);
            case "hidrologia" -> nivelDoCorrego(leitura, ref);
            default -> Optional.empty();
        };

        return alerta.map(a -> a
                .append("disparado_em", Date.from(momento))
                .append("reconhecido", false));
    }

    private Optional<Document> qualidadeDoAr(Map<String, Object> leitura) {
        Number iqar = numero(leitura, "indice_iqar");
        if (iqar == null) {
            return Optional.empty();
        }
        if (iqar.doubleValue() > IQAR_CRITICO) {
            return alerta("critico", "iqar > 130 - qualidade do ar ruim", iqar, IQAR_CRITICO);
        }
        if (iqar.doubleValue() > IQAR_ATENCAO) {
            return alerta("medio", "iqar > 80 - qualidade do ar moderada", iqar, IQAR_ATENCAO);
        }
        return Optional.empty();
    }

    /** NBR 10151: o limite depende do periodo (diurno/noturno) e vem na referencia da leitura. */
    private Optional<Document> ruido(Map<String, Object> leitura, Map<String, Object> ref) {
        Number laeq = numero(leitura, "laeq_db");
        String periodo = "noturno".equals(leitura.get("periodo")) ? "noturno" : "diurno";
        Number limite = numero(ref, "limite_" + periodo + "_db");
        if (laeq == null || limite == null || laeq.doubleValue() <= limite.doubleValue()) {
            return Optional.empty();
        }
        return alerta("alto", "LAeq " + periodo + " acima do limite da zona", laeq, limite);
    }

    private Optional<Document> perdasDeAgua(Map<String, Object> leitura, Map<String, Object> ref) {
        Number perdas = numero(leitura, "perdas_pct");
        Number meta = numero(ref, "meta_municipal_pct");
        if (perdas == null || meta == null || perdas.doubleValue() <= meta.doubleValue()) {
            return Optional.empty();
        }
        return alerta("alto", "perdas de distribuicao acima da meta municipal", perdas, meta);
    }

    /** Duas cotas: atencao (nivel alto) e alerta (nivel critico, risco de transbordo). */
    private Optional<Document> nivelDoCorrego(Map<String, Object> leitura, Map<String, Object> ref) {
        Number nivel = numero(leitura, "nivel_cm");
        if (nivel == null) {
            return Optional.empty();
        }
        Number cotaAlerta = numero(ref, "nivel_alerta_cm");
        Number cotaAtencao = numero(ref, "nivel_atencao_cm");
        if (cotaAlerta != null && nivel.doubleValue() >= cotaAlerta.doubleValue()) {
            return alerta("critico", "nivel do corrego na cota de alerta", nivel, cotaAlerta);
        }
        if (cotaAtencao != null && nivel.doubleValue() >= cotaAtencao.doubleValue()) {
            return alerta("alto", "nivel do corrego acima da cota de atencao", nivel, cotaAtencao);
        }
        return Optional.empty();
    }

    private static Optional<Document> alerta(String nivel, String regra, Number medido, Number limite) {
        return Optional.of(new Document("nivel", nivel)
                .append("regra", regra)
                .append("valor_medido", medido)
                .append("limite", limite));
    }

    /** Le um campo numerico; devolve null se ausente ou nao numerico (esquema flexivel). */
    private static Number numero(Map<String, Object> mapa, String campo) {
        return mapa.get(campo) instanceof Number n ? n : null;
    }
}
