#!/usr/bin/env bash
# =============================================================================
# GaiaOps ESG - captura de tela de um ambiente
#
# Uso:  deploy/captura-tela.sh <staging|production>
#
# Abre a pagina inicial (e o Swagger UI, fora de producao) num Chrome headless
# e salva PNGs em evidencias/<ambiente>/. Os runners do GitHub ja trazem o
# Google Chrome instalado; sem navegador, a captura e apenas ignorada.
# =============================================================================
set -euo pipefail

AMBIENTE="${1:-}"
RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="$RAIZ/deploy/$AMBIENTE.env"

if [[ -z "$AMBIENTE" || ! -f "$ENV_FILE" ]]; then
  echo "Uso: deploy/captura-tela.sh <staging|production>" >&2
  exit 2
fi

set -a
# shellcheck source=/dev/null
source "$ENV_FILE"
set +a

URL="${SMOKE_URL:-http://localhost:$APP_PORT}"
SAIDA="$RAIZ/evidencias/$AMBIENTE"
mkdir -p "$SAIDA"

NAVEGADOR="$(command -v google-chrome || command -v chromium || command -v chromium-browser || true)"
if [[ -z "$NAVEGADOR" ]]; then
  echo "Chrome/Chromium nao encontrado - captura de tela ignorada."
  exit 0
fi

capturar() {
  local caminho="$1" arquivo="$2"
  # --virtual-time-budget da tempo para o JavaScript da pagina buscar os dados na API.
  "$NAVEGADOR" --headless=new --no-sandbox --disable-gpu --hide-scrollbars \
    --window-size=1366,860 --virtual-time-budget=10000 \
    --screenshot="$SAIDA/$arquivo" "$URL$caminho" > /dev/null 2>&1
  echo "Captura salva: evidencias/$AMBIENTE/$arquivo  ($URL$caminho)"
}

capturar "/" "pagina-inicial.png"
if [[ "$SPRING_PROFILES_ACTIVE" != "prod" ]]; then
  capturar "/swagger-ui/index.html" "swagger-ui.png"
fi
