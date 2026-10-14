#!/usr/bin/env bash
# =============================================================================
# GaiaOps ESG - deploy de um ambiente com Docker Compose
#
# Uso:  deploy/deploy.sh <staging|production>
#
# O MESMO script e usado pelo pipeline (GitHub Actions) e localmente:
#   - com API_IMAGE definida (pipeline): baixa essa imagem do registry (GHCR)
#     e sobe. E exatamente a imagem que passou pelos testes - nada e
#     recompilado no deploy, e producao recebe a mesma imagem de staging;
#   - sem API_IMAGE (local): constroi a imagem a partir do Dockerfile.
#
# Cada ambiente e um "projeto" Compose separado (gaiaops-staging,
# gaiaops-production): containers, rede e volume proprios, em portas
# diferentes - os dois podem rodar lado a lado na mesma maquina.
#
# Senhas do MongoDB (MONGO_ROOT_PASSWORD e MONGO_APP_PASSWORD), em ordem:
#   1. variaveis de ambiente (no pipeline: GitHub Secrets do environment);
#   2. arquivo deploy/.segredos-<ambiente>.env, criado na primeira execucao
#      com senhas aleatorias e reaproveitado depois (esta no .gitignore).
# =============================================================================
set -euo pipefail

AMBIENTE="${1:-}"
RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="$RAIZ/deploy/$AMBIENTE.env"

if [[ -z "$AMBIENTE" || ! -f "$ENV_FILE" ]]; then
  echo "Uso: deploy/deploy.sh <staging|production>" >&2
  exit 2
fi

gerar_senha() {
  head -c 16 /dev/urandom | od -An -tx1 | tr -d ' \n'
}

# --- 1. Senhas -----------------------------------------------------------------
ARQ_SEGREDOS="$RAIZ/deploy/.segredos-$AMBIENTE.env"
if [[ -z "${MONGO_ROOT_PASSWORD:-}" || -z "${MONGO_APP_PASSWORD:-}" ]]; then
  if [[ ! -f "$ARQ_SEGREDOS" ]]; then
    echo ">> Senhas nao informadas: gerando ${ARQ_SEGREDOS#"$RAIZ"/} (fora do Git)"
    (umask 077 && printf 'MONGO_ROOT_PASSWORD=%s\nMONGO_APP_PASSWORD=%s\n' \
      "$(gerar_senha)" "$(gerar_senha)" > "$ARQ_SEGREDOS")
  fi
  # Le o arquivo sem sobrescrever uma senha que ja tenha vindo do ambiente.
  while IFS='=' read -r chave valor; do
    if [[ -z "${!chave:-}" ]]; then
      export "$chave=$valor"
    fi
  done < "$ARQ_SEGREDOS"
fi
export MONGO_ROOT_PASSWORD MONGO_APP_PASSWORD

# No GitHub Actions, garante que as senhas nunca aparecam no log.
if [[ "${GITHUB_ACTIONS:-}" == "true" ]]; then
  echo "::add-mask::$MONGO_ROOT_PASSWORD"
  echo "::add-mask::$MONGO_APP_PASSWORD"
fi

# Commit implantado: exibido em /actuator/info e na pagina inicial.
export APP_COMMIT="${APP_COMMIT:-$(git -C "$RAIZ" rev-parse HEAD 2>/dev/null || echo local)}"

# --- 2. Subida do ambiente ---------------------------------------------------------
COMPOSE=(docker compose
  --project-name "gaiaops-$AMBIENTE"
  --env-file "$ENV_FILE"
  --file "$RAIZ/docker-compose.yml")

# Se algo falhar, mostra o estado e o log dos containers antes de sair.
trap 'echo ">> FALHA no deploy de $AMBIENTE - estado e logs:"; "${COMPOSE[@]}" ps -a; "${COMPOSE[@]}" logs --no-color --tail 150' ERR

echo "=============================================================="
echo " Deploy GaiaOps ESG - ambiente: $AMBIENTE"
echo " Projeto Compose: gaiaops-$AMBIENTE"
echo " Commit:          $APP_COMMIT"
echo "=============================================================="

if [[ -n "${API_IMAGE:-}" ]]; then
  echo ">> Imagem do registry: $API_IMAGE"
  "${COMPOSE[@]}" pull api
  # --wait: so retorna quando todos os containers estao "healthy".
  "${COMPOSE[@]}" up --detach --no-build --wait --wait-timeout 240
else
  echo ">> Construindo a imagem a partir do Dockerfile"
  "${COMPOSE[@]}" up --detach --build --wait --wait-timeout 360
fi

echo
"${COMPOSE[@]}" ps
echo
echo ">> Ambiente '$AMBIENTE' no ar. Proximo passo: deploy/smoke-test.sh $AMBIENTE"
