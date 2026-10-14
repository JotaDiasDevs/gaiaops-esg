package br.com.fiap.gaiaops.medicao;

import static br.com.fiap.gaiaops.comum.Colecoes.EQUIPAMENTOS;
import static br.com.fiap.gaiaops.comum.Colecoes.MEDICOES;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import br.com.fiap.gaiaops.comum.Dominio;
import br.com.fiap.gaiaops.comum.RecursoNaoEncontradoException;
import br.com.fiap.gaiaops.comum.RegraNegocioException;

/** Telemetria dos sensores urbanos: consulta, alertas e ingestao de novas leituras. */
@Service
public class MedicaoService {

    private final MongoTemplate mongo;
    private final RegrasDeAlerta regras;
    private final Clock relogio;

    public MedicaoService(MongoTemplate mongo, RegrasDeAlerta regras, Clock relogio) {
        this.mongo = mongo;
        this.regras = regras;
        this.relogio = relogio;
    }

    public List<Document> listar(String equipamentoId, String tipo, int limite) {
        Query consulta = new Query();
        if (equipamentoId != null) {
            consulta.addCriteria(where("equipamento_id").is(equipamentoId));
        }
        if (tipo != null) {
            consulta.addCriteria(where("tipo").is(tipo));
        }
        consulta.with(Sort.by(Sort.Direction.DESC, "data")).limit(Dominio.limitar(limite));
        return mongo.find(consulta, Document.class, MEDICOES);
    }

    /**
     * Leituras que dispararam alerta.
     *
     * O filtro { alerta: { $exists: true } } e obrigatorio mesmo quando ha filtro
     * por nivel: e o predicado do indice parcial idx_medicao_alertas. Sem ele o
     * MongoDB ignora o indice e varre a collection inteira - a segunda armadilha
     * documentada na Fase 5 (Secao 11.1).
     */
    public List<Document> alertas(String nivel, boolean somenteAbertos) {
        Query consulta = query(where("alerta").exists(true));
        if (nivel != null) {
            consulta.addCriteria(where("alerta.nivel").is(nivel));
        }
        if (somenteAbertos) {
            consulta.addCriteria(where("alerta.reconhecido").is(false));
        }
        consulta.with(Sort.by(Sort.Direction.DESC, "data"));
        return mongo.find(consulta, Document.class, MEDICOES);
    }

    /** Registra uma leitura e aplica as regras de alerta automatico. */
    public Document registrar(NovaMedicao nova) {
        Document equipamento = mongo.findOne(query(where("codigo").is(nova.equipamentoId())),
                Document.class, EQUIPAMENTOS);
        if (equipamento == null) {
            throw new RecursoNaoEncontradoException("Equipamento " + nova.equipamentoId() + " nao encontrado");
        }
        if (!Boolean.TRUE.equals(equipamento.getBoolean("ativo"))) {
            throw new RegraNegocioException("Equipamento " + nova.equipamentoId()
                    + " esta desativado e nao recebe medicoes");
        }

        Instant momento = nova.data() != null ? nova.data() : relogio.instant();
        Document medicao = new Document("equipamento_id", nova.equipamentoId())
                .append("tipo", nova.tipo())
                .append("data", Date.from(momento))
                .append("competencia", YearMonth.from(momento.atZone(ZoneOffset.UTC)).toString());
        if (nova.sensor() != null) {
            medicao.append("sensor", nova.sensor());
        }
        medicao.append("leitura", nova.leitura());
        if (nova.referencia() != null) {
            medicao.append("referencia", nova.referencia());
        }

        // O campo "alerta" so existe quando uma regra dispara: esquema flexivel na pratica.
        regras.avaliar(nova.tipo(), nova.leitura(), nova.referencia(), relogio.instant())
                .ifPresent(alerta -> medicao.append("alerta", alerta));

        return mongo.insert(medicao, MEDICOES);
    }
}
