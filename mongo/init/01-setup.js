/* =============================================================================
 * GaiaOps ESG - inicializacao do MongoDB no container
 *
 * Executado pelo entrypoint oficial da imagem "mongo" UMA unica vez: quando o
 * volume de dados ainda esta vazio (primeiro "docker compose up" do ambiente).
 * Reinicios e novos deploys reaproveitam o volume e nao passam por aqui.
 *
 * O que faz:
 *   1. cria o usuario da aplicacao, com permissao apenas de leitura/escrita
 *      no banco da aplicacao (o usuario root fica restrito a administracao);
 *   2. cria as 5 collections com os validadores $jsonSchema;
 *   3. cria os indices.
 *
 * Os itens 2 e 3 sao os mesmos de scripts/00-setup.mongodb.js da Fase 5, sem a
 * etapa de limpeza (aqui o banco e sempre novo). Os DADOS nao sao inseridos
 * aqui: a carga inicial e feita pela propria API (classe CargaInicial).
 *
 * Variaveis lidas do ambiente do container (definidas no .env):
 *   MONGO_INITDB_DATABASE, MONGO_APP_USER, MONGO_APP_PASSWORD
 * =========================================================================== */

const nomeBanco = process.env.MONGO_INITDB_DATABASE || "gaiaops_esg";
const banco = db.getSiblingDB(nomeBanco);

print("[gaiaops-init] banco: " + nomeBanco);

/* --- 1. Usuario da aplicacao (principio do menor privilegio) -------------- */
const usuario = process.env.MONGO_APP_USER;
const senha = process.env.MONGO_APP_PASSWORD;

if (usuario && senha) {
  if (banco.getUser(usuario) === null) {
    banco.createUser({
      user: usuario,
      pwd: senha,
      roles: [{ role: "readWrite", db: nomeBanco }]
    });
    print("[gaiaops-init] usuario da aplicacao criado: " + usuario);
  }
} else {
  print("[gaiaops-init] MONGO_APP_USER/MONGO_APP_PASSWORD ausentes - usuario nao criado");
}

/* --- 2. Collections e validadores ---------------------------------------- */
function criarCollection(nome, opcoes) {
  if (banco.getCollectionNames().indexOf(nome) >= 0) {
    print("[gaiaops-init] collection " + nome + " ja existe - mantida");
    return;
  }
  banco.createCollection(nome, opcoes);
  print("[gaiaops-init] collection " + nome + " criada");
}

// equipamentos: validacao ESTRITA - e a espinha dorsal referenciada pelas demais.
criarCollection("equipamentos", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      title: "Equipamento publico do municipio",
      required: ["codigo", "nome", "tipo", "distrito", "ativo"],
      properties: {
        codigo: { bsonType: "string", pattern: "^EQP-[A-Z]{3}-[0-9]{3}$" },
        nome: { bsonType: "string", minLength: 3 },
        tipo: {
          bsonType: "string",
          enum: ["escola", "saude", "praca", "saneamento", "iluminacao", "ecoponto", "terminal"]
        },
        distrito: { bsonType: "string", minLength: 3 },
        ativo: { bsonType: "bool" },
        populacao_atendida: { bsonType: ["int", "long"], minimum: 0 }
      }
    }
  },
  validationLevel: "strict",
  validationAction: "error"
});

// As demais: validacao MODERADA, so sobre o campo discriminador (esquema flexivel).
criarCollection("medicoes", {
  validator: { $jsonSchema: {
    bsonType: "object",
    required: ["equipamento_id", "tipo", "data"],
    properties: { tipo: { bsonType: "string",
      enum: ["qualidade_ar", "ruido", "energia", "agua", "trafego", "hidrologia"] } }
  } },
  validationLevel: "moderate",
  validationAction: "warn"
});

criarCollection("residuos", {
  validator: { $jsonSchema: {
    bsonType: "object",
    required: ["categoria", "data_coleta"],
    properties: { categoria: { bsonType: "string",
      enum: ["reciclavel", "organico", "rejeito", "eletronico", "saude", "construcao"] } }
  } },
  validationLevel: "moderate",
  validationAction: "warn"
});

criarCollection("programas_sociais", {
  validator: { $jsonSchema: {
    bsonType: "object",
    required: ["codigo", "nome", "eixo"],
    properties: { eixo: { bsonType: "string",
      enum: ["assistencia", "educacao", "saude", "trabalho_renda", "acessibilidade", "habitacao"] } }
  } },
  validationLevel: "moderate",
  validationAction: "warn"
});

criarCollection("conformidade", {
  validator: { $jsonSchema: {
    bsonType: "object",
    required: ["tipo"],
    properties: { tipo: { bsonType: "string",
      enum: ["licenca", "audiencia_publica", "conselho", "ouvidoria", "termo_ajustamento"] } }
  } },
  validationLevel: "moderate",
  validationAction: "warn"
});

/* --- 3. Indices (createIndex e idempotente) ------------------------------ */
banco.equipamentos.createIndex({ codigo: 1 }, { unique: true, name: "idx_equipamento_codigo_unico" });
banco.equipamentos.createIndex({ local: "2dsphere" }, { name: "idx_equipamento_geo" });
banco.equipamentos.createIndex({ tipo: 1, distrito: 1 }, { name: "idx_equipamento_tipo_distrito" });

banco.medicoes.createIndex({ equipamento_id: 1, tipo: 1, data: -1 }, { name: "idx_medicao_equip_tipo_data" });
banco.medicoes.createIndex({ "alerta.nivel": 1, data: -1 },
  { name: "idx_medicao_alertas", partialFilterExpression: { alerta: { $exists: true } } });
banco.medicoes.createIndex({ "sensor.id": 1, data: -1 }, { name: "idx_medicao_sensor" });

banco.residuos.createIndex({ equipamento_id: 1, data_coleta: -1 }, { name: "idx_residuo_equip_data" });
banco.residuos.createIndex({ categoria: 1, "destinacao.tipo": 1 }, { name: "idx_residuo_categoria_destinacao" });
banco.residuos.createIndex({ "mtr.numero": 1 }, { unique: true, sparse: true, name: "idx_residuo_manifesto_unico" });

banco.programas_sociais.createIndex({ codigo: 1 }, { unique: true, name: "idx_programa_codigo_unico" });
banco.programas_sociais.createIndex({ eixo: 1, "cobertura.distrito": 1 }, { name: "idx_programa_eixo_distrito" });
banco.programas_sociais.createIndex({ "beneficiarios.situacao": 1 }, { name: "idx_programa_beneficiarios" });

banco.conformidade.createIndex({ tipo: 1, equipamento_id: 1 }, { name: "idx_conformidade_tipo_equipamento" });
banco.conformidade.createIndex({ validade: 1 },
  { name: "idx_conformidade_validade", partialFilterExpression: { tipo: "licenca" } });
banco.conformidade.createIndex({ descricao: "text", assunto: "text" },
  { name: "idx_conformidade_texto", default_language: "portuguese" });

print("[gaiaops-init] indices criados");
print("[gaiaops-init] collections: " + JSON.stringify(banco.getCollectionNames().sort()));
print("[gaiaops-init] concluido");
