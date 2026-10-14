package br.com.fiap.gaiaops.programa;

import static br.com.fiap.gaiaops.comum.Colecoes.PROGRAMAS_SOCIAIS;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import java.util.List;

import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import br.com.fiap.gaiaops.comum.RecursoNaoEncontradoException;

/** Programas sociais do municipio e seus beneficiarios (pseudonimizados por protocolo - LGPD). */
@Service
public class ProgramaSocialService {

    private final MongoTemplate mongo;

    public ProgramaSocialService(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    public List<Document> listar(String eixo) {
        Query consulta = new Query();
        if (eixo != null) {
            consulta.addCriteria(where("eixo").is(eixo));
        }
        return mongo.find(consulta.with(Sort.by("codigo")), Document.class, PROGRAMAS_SOCIAIS);
    }

    public Document buscar(String codigo) {
        Document programa = mongo.findOne(query(where("codigo").is(codigo)), Document.class, PROGRAMAS_SOCIAIS);
        if (programa == null) {
            throw new RecursoNaoEncontradoException("Programa " + codigo + " nao encontrado");
        }
        return programa;
    }
}
