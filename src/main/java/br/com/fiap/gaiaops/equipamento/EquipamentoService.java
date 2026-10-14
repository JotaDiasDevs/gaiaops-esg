package br.com.fiap.gaiaops.equipamento;

import static br.com.fiap.gaiaops.comum.Colecoes.EQUIPAMENTOS;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import java.time.Clock;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.mongodb.client.result.UpdateResult;

import br.com.fiap.gaiaops.comum.ConflitoException;
import br.com.fiap.gaiaops.comum.Dominio;
import br.com.fiap.gaiaops.comum.RecursoNaoEncontradoException;
import br.com.fiap.gaiaops.comum.RegraNegocioException;

/**
 * Regras do cadastro de equipamentos publicos.
 *
 * Trabalha com org.bson.Document (e nao com uma classe fixa) para nao perder
 * os campos especificos de cada tipo de equipamento ao ler ou gravar.
 */
@Service
public class EquipamentoService {

    /** Campos controlados pela API: nao podem ser sobrescritos via "atributos". */
    private static final Set<String> CAMPOS_CONTROLADOS = Set.of(
            "_id", "codigo", "nome", "tipo", "distrito", "populacao_atendida",
            "ativo", "criado_em", "atualizado_em", "desativado_em");

    private final MongoTemplate mongo;
    private final Clock relogio;

    public EquipamentoService(MongoTemplate mongo, Clock relogio) {
        this.mongo = mongo;
        this.relogio = relogio;
    }

    public List<Document> listar(String tipo, String distrito, Boolean ativo, int limite) {
        Query consulta = new Query();
        if (tipo != null) {
            consulta.addCriteria(where("tipo").is(tipo));
        }
        if (distrito != null) {
            consulta.addCriteria(where("distrito").is(distrito));
        }
        if (ativo != null) {
            consulta.addCriteria(where("ativo").is(ativo));
        }
        consulta.with(Sort.by("codigo")).limit(Dominio.limitar(limite));
        return mongo.find(consulta, Document.class, EQUIPAMENTOS);
    }

    public Document buscar(String codigo) {
        Document equipamento = mongo.findOne(porCodigo(codigo), Document.class, EQUIPAMENTOS);
        if (equipamento == null) {
            throw new RecursoNaoEncontradoException("Equipamento " + codigo + " nao encontrado");
        }
        return equipamento;
    }

    public Document criar(NovoEquipamento novo) {
        if (mongo.exists(porCodigo(novo.codigo()), EQUIPAMENTOS)) {
            throw new ConflitoException("Ja existe equipamento com o codigo " + novo.codigo());
        }

        // Atributos livres primeiro; os campos controlados vem depois e prevalecem.
        Document equipamento = new Document(atributosLivres(novo.atributos()))
                .append("codigo", novo.codigo())
                .append("nome", novo.nome())
                .append("tipo", novo.tipo())
                .append("distrito", novo.distrito())
                .append("ativo", true)
                .append("criado_em", agora());
        if (novo.populacaoAtendida() != null) {
            equipamento.append("populacao_atendida", novo.populacaoAtendida());
        }

        return mongo.insert(equipamento, EQUIPAMENTOS);
    }

    public Document atualizar(String codigo, AtualizacaoEquipamento dados) {
        Update update = new Update()
                .set("nome", dados.nome())
                .set("tipo", dados.tipo())
                .set("distrito", dados.distrito())
                .set("atualizado_em", agora());
        if (dados.populacaoAtendida() != null) {
            update.set("populacao_atendida", dados.populacaoAtendida());
        }
        atributosLivres(dados.atributos()).forEach(update::set);

        Document atualizado = mongo.findAndModify(porCodigo(codigo), update,
                FindAndModifyOptions.options().returnNew(true), Document.class, EQUIPAMENTOS);
        if (atualizado == null) {
            throw new RecursoNaoEncontradoException("Equipamento " + codigo + " nao encontrado");
        }
        return atualizado;
    }

    /**
     * Exclusao logica (soft delete), como na Fase 5: o equipamento e referenciado
     * por medicoes, residuos e licencas, que perderiam o contexto se ele sumisse.
     */
    public void desativar(String codigo) {
        UpdateResult resultado = mongo.updateFirst(porCodigo(codigo),
                new Update().set("ativo", false).set("desativado_em", agora()), EQUIPAMENTOS);
        if (resultado.getMatchedCount() == 0) {
            throw new RecursoNaoEncontradoException("Equipamento " + codigo + " nao encontrado");
        }
    }

    /** Remove campos controlados e recusa nomes que o MongoDB nao aceita como campo. */
    private static Map<String, Object> atributosLivres(Map<String, Object> atributos) {
        if (atributos == null) {
            return Map.of();
        }
        atributos.keySet().forEach(campo -> {
            if (campo.isBlank() || campo.startsWith("$") || campo.contains(".")) {
                throw new RegraNegocioException("Nome de atributo invalido: '" + campo + "'");
            }
        });
        Map<String, Object> livres = new LinkedHashMap<>(atributos);
        livres.keySet().removeAll(CAMPOS_CONTROLADOS);
        return livres;
    }

    private static Query porCodigo(String codigo) {
        return query(where("codigo").is(codigo));
    }

    private Date agora() {
        return Date.from(relogio.instant());
    }
}
