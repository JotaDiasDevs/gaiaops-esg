"""
GaiaOps ESG - resumo dos testes para a pagina da execucao (Job Summary).

Le os relatorios XML do Maven (surefire = unitarios, failsafe = integracao) e o
CSV de cobertura do JaCoCo, e escreve uma tabela em Markdown em
$GITHUB_STEP_SUMMARY. Fora do GitHub Actions, imprime no terminal.

Uso (na raiz do projeto, depois de ./mvnw verify):
    python3 .github/scripts/resumo_testes.py
"""
import csv
import glob
import os
import xml.etree.ElementTree as ET

RELATORIOS = [
    ("Unitarios", "target/surefire-reports/TEST-*.xml"),
    ("Integracao", "target/failsafe-reports/TEST-*.xml"),
]
COBERTURA = "target/site/jacoco/jacoco.csv"


def ler_suites(padrao):
    """Uma linha por classe de teste: (classe, total, falhas, ignorados, segundos)."""
    suites = []
    for arquivo in sorted(glob.glob(padrao)):
        raiz = ET.parse(arquivo).getroot()
        suites.append((
            raiz.get("name").split(".")[-1],
            int(raiz.get("tests", 0)),
            int(raiz.get("failures", 0)) + int(raiz.get("errors", 0)),
            int(raiz.get("skipped", 0)),
            float(raiz.get("time", 0)),
        ))
    return suites


def cobertura_de_linhas():
    if not os.path.exists(COBERTURA):
        return None
    cobertas = perdidas = 0
    with open(COBERTURA, encoding="utf-8") as arquivo:
        for linha in csv.DictReader(arquivo):
            cobertas += int(linha["LINE_COVERED"])
            perdidas += int(linha["LINE_MISSED"])
    total = cobertas + perdidas
    return 100 * cobertas / total if total else 0.0


def main():
    linhas = ["## Testes automatizados", "",
              "| Tipo | Classe | Testes | Falhas | Ignorados | Tempo (s) |",
              "|---|---|---:|---:|---:|---:|"]
    total = falhas = ignorados = 0

    for tipo, padrao in RELATORIOS:
        for classe, n, f, s, t in ler_suites(padrao):
            icone = ":x:" if f else (":fast_forward:" if s == n else ":white_check_mark:")
            linhas.append(f"| {tipo} | {icone} `{classe}` | {n} | {f} | {s} | {t:.2f} |")
            total, falhas, ignorados = total + n, falhas + f, ignorados + s

    aprovados = total - falhas - ignorados
    linhas += ["", f"**{aprovados} aprovados, {falhas} falhas, {ignorados} ignorados** (total: {total})"]

    cobertura = cobertura_de_linhas()
    if cobertura is not None:
        linhas.append(f"\nCobertura de linhas (JaCoCo): **{cobertura:.1f}%**")

    texto = "\n".join(linhas) + "\n"
    destino = os.environ.get("GITHUB_STEP_SUMMARY")
    if destino:
        with open(destino, "a", encoding="utf-8") as arquivo:
            arquivo.write(texto)
    print(texto)


if __name__ == "__main__":
    main()
