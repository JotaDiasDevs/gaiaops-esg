package br.com.fiap.gaiaops.carga;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import br.com.fiap.gaiaops.comum.Colecoes;

/**
 * Carga inicial da base com os 100 documentos da Fase 5.
 *
 * Os arquivos em resources/seed foram exportados com mongoexport da base
 * gaiaops_esg (um documento Extended JSON por linha), entao _id, datas e
 * subdocumentos chegam identicos ao original.
 *
 * Idempotente: so popula a collection que estiver vazia. Assim, reiniciar o
 * container ou fazer um novo deploy nunca duplica nem sobrescreve dados.
 * Desligue com GAIAOPS_SEED_ENABLED=false.
 */
@Component
@ConditionalOnProperty(name = "gaiaops.seed.enabled", havingValue = "true")
public class CargaInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CargaInicial.class);

    private final MongoTemplate mongo;

    public CargaInicial(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        for (String colecao : Colecoes.TODAS) {
            long existentes = mongo.getCollection(colecao).countDocuments();
            if (existentes > 0) {
                log.info("Carga inicial: '{}' ja possui {} documentos - mantida como esta", colecao, existentes);
                continue;
            }
            List<Document> documentos = lerSeed(colecao);
            mongo.getCollection(colecao).insertMany(documentos);
            log.info("Carga inicial: '{}' populada com {} documentos", colecao, documentos.size());
        }
    }

    private List<Document> lerSeed(String colecao) throws IOException {
        ClassPathResource arquivo = new ClassPathResource("seed/" + colecao + ".json");
        try (BufferedReader leitor = new BufferedReader(
                new InputStreamReader(arquivo.getInputStream(), StandardCharsets.UTF_8))) {
            return leitor.lines()
                    .filter(linha -> !linha.isBlank())
                    .map(Document::parse)
                    .toList();
        }
    }
}
