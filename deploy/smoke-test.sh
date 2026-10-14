#!/usr/bin/env bash
# =============================================================================
# GaiaOps ESG - smoke test de um ambiente recem-implantado
#
# Uso:  deploy/smoke-test.sh <staging|production>
#
# Faz requisicoes reais ao ambiente e confere o essencial: aplicacao pronta,
# banco conectado, ambiente/commit corretos, API respondendo com dados e a
# configuracao especifica do ambiente aplicada. Qualquer falha encerra com
# codigo diferente de zero - no pipeline, isso BLOQUEIA a promocao para
# producao.
#
# As respostas ficam em evidencias/<ambiente>/ (prova de funcionamento) e, no
# GitHub Actions, um resumo vai para a pagina da execucao (Job Summary).
# =============================================================================
set -uo pipefail

AMBIENTE="${1:-}"
RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="$RAIZ/deploy/$AMBIENTE.env"

if [[ -z "$AMBIENTE" || ! -f "$ENV_FILE" ]]; then
  echo "Uso: deploy/smoke-test.sh <staging|production>" >&2
  exit 2
fi

# Le APP_PORT, GAIAOPS_AMBIENTE e SPRING_PROFILES_ACTIVE do arquivo do ambiente.
set -a
# shellcheck source=/dev/null
source "$ENV_FILE"
set +a

URL="${SMOKE_URL:-http://localhost:$APP_PORT}"
SAIDA="$RAIZ/evidencias/$AMBIENTE"
mkdir -p "$SAIDA"

falhas=0
resultados=()

checar() {
  local descricao="$1"
  shift
  if "$@" > /dev/null 2>&1; then
    echo "  [ OK  ] $descricao"
    resultados+=("| :white_check_mark: | $descricao |")
  else
    echo "  [FALHA] $descricao"
    resultados+=("| :x: | $descricao |")
    falhas=$((falhas + 1))
  fi
}

# GET que grava a resposta em evidencias/ e confere um trecho esperado.
get_contem() {
  local caminho="$1" arquivo="$2" trecho="$3"
  curl -fsS --max-time 10 "$URL$caminho" -o "$SAIDA/$arquivo" && grep -q "$trecho" "$SAIDA/$arquivo"
}

status_http() {
  curl -s -o /dev/null -w '%{http_code}' --max-time 10 "$@"
}

echo "=============================================================="
echo " Smoke test - ambiente: $AMBIENTE  ($URL)"
echo "=============================================================="

# Aguarda a aplicacao ficar pronta (ate 90 s) - util quando rodado isoladamente.
for _ in $(seq 1 45); do
  [[ "$(status_http "$URL/actuator/health/readiness")" == "200" ]] && break
  sleep 2
done

checar "Aplicacao pronta e MongoDB conectado (/actuator/health/readiness = UP)" \
  get_contem "/actuator/health/readiness" "health.json" '"status":"UP"'

checar "Ambiente correto em /actuator/info (\"$GAIAOPS_AMBIENTE\")" \
  get_contem "/actuator/info" "info.json" "\"ambiente\":\"$GAIAOPS_AMBIENTE\""

if [[ -n "${APP_COMMIT:-}" ]]; then
  checar "Commit implantado e o esperado (${APP_COMMIT:0:7})" \
    grep -q "\"commit\":\"$APP_COMMIT\"" "$SAIDA/info.json"
fi

checar "Painel ESG calculado (/api/indicadores)" \
  get_contem "/api/indicadores" "indicadores.json" '"ambiental"'

checar "Carga inicial presente (/api/equipamentos)" \
  get_contem "/api/equipamentos?limite=5" "equipamentos.json" '"codigo":"EQP-'

checar "Validacao ativa: cadastro invalido recusado com HTTP 400" \
  test "$(status_http -X POST -H 'Content-Type: application/json' \
          -d '{"codigo":"invalido"}' "$URL/api/equipamentos")" == "400"

checar "Pagina inicial publicada (/)" \
  get_contem "/" "index.html" "GaiaOps ESG"

# Configuracao especifica do ambiente: Swagger so fora de producao.
if [[ "$SPRING_PROFILES_ACTIVE" == "prod" ]]; then
  checar "Perfil prod aplicado: Swagger desabilitado (HTTP 404)" \
    test "$(status_http "$URL/v3/api-docs")" == "404"
else
  checar "Perfil $SPRING_PROFILES_ACTIVE aplicado: Swagger disponivel (HTTP 200)" \
    test "$(status_http "$URL/v3/api-docs")" == "200"
fi

total=${#resultados[@]}
echo
echo ">> $((total - falhas))/$total verificacoes OK. Respostas salvas em ${SAIDA#"$RAIZ"/}/"

# --- Resumo na pagina da execucao do GitHub Actions --------------------------
if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
  {
    echo "## Smoke test - $AMBIENTE"
    echo
    echo "| | Verificacao |"
    echo "|---|---|"
    printf '%s\n' "${resultados[@]}"
    echo
    echo "**$((total - falhas))/$total verificacoes OK** em \`$URL\`"
    echo
    echo "<details><summary>/actuator/info</summary>"
    echo
    echo '```json'
    python3 -m json.tool "$SAIDA/info.json" 2>/dev/null || cat "$SAIDA/info.json" 2>/dev/null
    echo '```'
    echo "</details>"
    echo
    echo "<details><summary>/api/indicadores (painel ESG)</summary>"
    echo
    echo '```json'
    python3 -m json.tool "$SAIDA/indicadores.json" 2>/dev/null || cat "$SAIDA/indicadores.json" 2>/dev/null
    echo '```'
    echo "</details>"
  } >> "$GITHUB_STEP_SUMMARY"
fi

[[ $falhas -eq 0 ]]
