package br.com.fiap.gaiaops.residuo;

import static br.com.fiap.gaiaops.comum.Colecoes.RESIDUOS;
import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import br.com.fiap.gaiaops.comum.Dominio;

/** Coleta seletiva e destinacao dos residuos do municipio. */
@Service
public class ResiduoService {

    private final MongoTemplate mongo;

    public ResiduoService(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    public List<Document> listar(String categoria, int limite) {
        Query consulta = new Query();
        if (categoria != null) {
            consulta.addCriteria(where("categoria").is(categoria));
        }
        consulta.with(Sort.by(Sort.Direction.DESC, "data_coleta")).limit(Dominio.limitar(limite));
        return mongo.find(consulta, Document.class, RESIDUOS);
    }

    /** Peso coletado por categoria e destinacao - a mesma agregacao da Fase 5, executada pela API. */
    public List<Document> resumoPorCategoria() {
        List<Document> pipeline = List.of(
                Document.parse("""
                        { "$group": {
                            "_id": { "categoria": "$categoria", "destinacao": "$destinacao.tipo" },
                            "coletas": { "$sum": 1 },
                            "peso_kg": { "$sum": "$peso_kg" } } }"""),
                Document.parse("""
                        { "$project": {
                            "_id": 0,
                            "categoria": "$_id.categoria",
                            "destinacao": "$_id.destinacao",
                            "coletas": 1,
                            "peso_kg": 1 } }"""),
                Document.parse("""
                        { "$sort": { "peso_kg": -1 } }"""));
        return mongo.getCollection(RESIDUOS).aggregate(pipeline).into(new ArrayList<>());
    }
}
