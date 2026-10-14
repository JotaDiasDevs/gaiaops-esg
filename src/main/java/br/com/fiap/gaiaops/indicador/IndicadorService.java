package br.com.fiap.gaiaops.indicador;

import static br.com.fiap.gaiaops.comum.Colecoes.CONFORMIDADE;
import static br.com.fiap.gaiaops.comum.Colecoes.EQUIPAMENTOS;
import static br.com.fiap.gaiaops.comum.Colecoes.MEDICOES;
import static br.com.fiap.gaiaops.comum.Colecoes.PROGRAMAS_SOCIAIS;
import static br.com.fiap.gaiaops.comum.Colecoes.RESIDUOS;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

/**
 * Calcula o painel ESG do municipio.
 *
 * As agregacoes sao escritas na mesma sintaxe usada no mongosh na Fase 5
 * (Document.parse), para que possam ser copiadas e conferidas diretamente
 * no shell. O calculo acontece no banco; a API so formata o resultado.
 */
@Service
public class IndicadorService {

    /** Destinacoes que evitam o aterro: entram na taxa de desvio. */
    private static final String DESTINACAO_SUSTENTAVEL = "reciclagem|compostagem";

    private final MongoTemplate mongo;
    private final Clock relogio;

    public IndicadorService(MongoTemplate mongo, Clock relogio) {
        this.mongo = mongo;
        this.relogio = relogio;
    }

    public PainelEsg painel() {
        Instant agora = relogio.instant();
        return new PainelEsg(agora, ambiental(), social(), governanca(agora));
    }

    // ------------------------------------------------------------------ E

    private PainelEsg.Ambiental ambiental() {
        Map<String, Long> alertasPorNivel = new LinkedHashMap<>();
        agregar(MEDICOES,
                """
                { "$match": { "alerta": { "$exists": true }, "alerta.reconhecido": false } }""",
                """
                { "$group": { "_id": "$alerta.nivel", "total": { "$sum": 1 } } }""",
                """
                { "$sort": { "total": -1 } }""")
                .forEach(d -> alertasPorNivel.put(d.getString("_id"), numero(d, "total").longValue()));

        Document residuos = primeiro(agregar(RESIDUOS, """
                { "$group": {
                    "_id": null,
                    "total_kg": { "$sum": "$peso_kg" },
                    "desviado_kg": { "$sum": { "$cond": [
                        { "$regexMatch": {
                            "input": { "$ifNull": ["$destinacao.tipo", ""] },
                            "regex": "%s" } },
                        "$peso_kg", 0 ] } } } }""".formatted(DESTINACAO_SUSTENTAVEL)));

        double totalKg = numero(residuos, "total_kg").doubleValue();
        double desviadoKg = numero(residuos, "desviado_kg").doubleValue();

        return new PainelEsg.Ambiental(
                contar(MEDICOES, new Query()),
                alertasPorNivel.values().stream().mapToLong(Long::longValue).sum(),
                alertasPorNivel,
                totalKg,
                percentual(desviadoKg, totalKg));
    }

    // ------------------------------------------------------------------ S

    private PainelEsg.Social social() {
        Document programas = primeiro(agregar(PROGRAMAS_SOCIAIS,
                """
                { "$match": { "ativo": true } }""",
                """
                { "$group": {
                    "_id": null,
                    "programas": { "$sum": 1 },
                    "beneficiarios": { "$sum": { "$size": { "$ifNull": ["$beneficiarios", []] } } },
                    "previsto": { "$sum": "$orcamento.previsto_brl" },
                    "executado": { "$sum": "$orcamento.executado_brl" } } }"""));

        double previsto = numero(programas, "previsto").doubleValue();
        double executado = numero(programas, "executado").doubleValue();

        return new PainelEsg.Social(
                numero(programas, "programas").longValue(),
                numero(programas, "beneficiarios").longValue(),
                previsto,
                executado,
                percentual(executado, previsto));
    }

    // ------------------------------------------------------------------ G

    private PainelEsg.Governanca governanca(Instant agora) {
        Date hoje = Date.from(agora);
        Date daqui180Dias = Date.from(agora.plus(180, ChronoUnit.DAYS));

        return new PainelEsg.Governanca(
                contar(EQUIPAMENTOS, query(where("ativo").is(true))),
                contar(EQUIPAMENTOS, new Query()),
                contar(CONFORMIDADE, query(where("tipo").is("licenca").and("validade").lt(hoje))),
                contar(CONFORMIDADE, query(where("tipo").is("licenca").and("validade").gte(hoje).lte(daqui180Dias))),
                contar(CONFORMIDADE, query(where("tipo").is("ouvidoria"))),
                contar(CONFORMIDADE, query(where("tipo").is("termo_ajustamento"))));
    }

    // ------------------------------------------------------------ apoio

    private long contar(String colecao, Query consulta) {
        return mongo.count(consulta, colecao);
    }

    private List<Document> agregar(String colecao, String... estagios) {
        List<Document> pipeline = Arrays.stream(estagios).map(Document::parse).toList();
        return mongo.getCollection(colecao).aggregate(pipeline).into(new ArrayList<>());
    }

    /** $group com _id null devolve um unico documento - ou nenhum, se a collection estiver vazia. */
    private static Document primeiro(List<Document> resultado) {
        return resultado.isEmpty() ? new Document() : resultado.get(0);
    }

    /** $sum pode devolver Integer, Long ou Double conforme os dados; ausente conta como zero. */
    private static Number numero(Document documento, String campo) {
        return documento.get(campo) instanceof Number n ? n : 0;
    }

    private static double percentual(double parte, double total) {
        return total == 0 ? 0 : Math.round(parte / total * 1000) / 10.0;
    }
}
