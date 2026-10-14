#!/usr/bin/env bash
# =============================================================================
# GaiaOps ESG - coleta de estado e logs dos containers de um ambiente
#
# Uso:  deploy/coletar-logs.sh <staging|production>
#
# Grava em evidencias/<ambiente>/:
#   containers.txt - containers, volumes e redes do ambiente
#   containers.log - log completo de cada container (API e MongoDB)
#
# Usa o label que o Compose coloca em tudo que cria
# (com.docker.compose.project), entao nao depende das variaveis do .env.
# =============================================================================
set -uo pipefail

AMBIENTE="${1:?Uso: deploy/coletar-logs.sh <staging|production>}"
RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FILTRO="label=com.docker.compose.project=gaiaops-$AMBIENTE"
SAIDA="$RAIZ/evidencias/$AMBIENTE"
mkdir -p "$SAIDA"

{
  echo "# Containers"
  docker ps -a --filter "$FILTRO" --format 'table {{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}'
  echo
  echo "# Volumes"
  docker volume ls --filter "$FILTRO"
  echo
  echo "# Redes"
  docker network ls --filter "$FILTRO"
} > "$SAIDA/containers.txt"
cat "$SAIDA/containers.txt"

for container in $(docker ps -a --filter "$FILTRO" --format '{{.Names}}'); do
  echo "===================== $container ====================="
  docker logs --timestamps "$container" 2>&1
  echo
done > "$SAIDA/containers.log"

echo
echo "Logs salvos em evidencias/$AMBIENTE/containers.log"

if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
  {
    echo "## Containers em execucao - $AMBIENTE"
    echo
    echo '```'
    cat "$SAIDA/containers.txt"
    echo '```'
  } >> "$GITHUB_STEP_SUMMARY"
fi
