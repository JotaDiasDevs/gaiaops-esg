package br.com.fiap.gaiaops.conformidade;

import static br.com.fiap.gaiaops.comum.Colecoes.CONFORMIDADE;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

/** Governanca municipal: licencas, audiencias, conselhos, ouvidoria e TACs. */
@Service
public class ConformidadeService {

    private final MongoTemplate mongo;
    private final Clock relogio;

    public ConformidadeService(MongoTemplate mongo, Clock relogio) {
        this.mongo = mongo;
        this.relogio = relogio;
    }

    public List<Document> listar(String tipo) {
        Query consulta = new Query();
        if (tipo != null) {
            consulta.addCriteria(where("tipo").is(tipo));
        }
        return mongo.find(consulta, Document.class, CONFORMIDADE);
    }

    /**
     * Licencas vencidas ou que vencem nos proximos "dias".
     *
     * O filtro tipo = "licenca" garante o uso do indice parcial
     * idx_conformidade_validade. Os campos dias_para_vencer e vencida sao
     * calculados na hora da consulta - o campo "situacao" gravado no documento
     * pode estar desatualizado (ex.: uma licenca "a_vencer" que ja venceu).
     */
    public List<Document> licencasComVencimentoAte(int dias) {
        Instant agora = relogio.instant();
        Date limite = Date.from(agora.plus(Math.max(dias, 0), ChronoUnit.DAYS));

        Query consulta = query(where("tipo").is("licenca").and("validade").lte(limite))
                .with(Sort.by("validade"));
        List<Document> licencas = mongo.find(consulta, Document.class, CONFORMIDADE);

        licencas.forEach(licenca -> {
            long diasParaVencer = ChronoUnit.DAYS.between(agora, licenca.getDate("validade").toInstant());
            licenca.append("dias_para_vencer", diasParaVencer)
                    .append("vencida", diasParaVencer < 0);
        });
        return licencas;
    }
}
