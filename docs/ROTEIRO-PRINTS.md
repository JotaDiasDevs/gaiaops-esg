# Roteiro de prints

Salve cada captura em `docs/prints/` **com o nome exato** indicado. O README já aponta para esses arquivos, e o gerador do PDF (`py tools/gerar_documento.py`) troca cada quadro de "print pendente" pela imagem correspondente.

Quando tudo estiver no lugar, gere o PDF e o .zip de novo (veja o fim deste roteiro).

## Antes de começar

1. Faça o push do projeto para o GitHub, na branch `main`.
2. Configure os environments (README, seção *Configuração no GitHub*): `staging` e `production`, com *Required reviewers* em `production`.
3. Abra a aba **Actions** e acompanhe a execução **CI/CD**. Quando o job *5. Deploy producao* pedir aprovação, aprove. Antes disso, tire o print 06.

## Prints gerados automaticamente pelo pipeline

Na página da execução, role até **Artifacts** e baixe `evidencias-staging` e `evidencias-production`.

| Arquivo em docs/prints/ | Origem |
|---|---|
| `08-staging-pagina-inicial.png` | `evidencias-staging/pagina-inicial.png` |
| `09-producao-pagina-inicial.png` | `evidencias-production/pagina-inicial.png` |
| `10-staging-swagger.png` | `evidencias-staging/swagger-ui.png` |

Os artefatos também trazem `info.json`, `indicadores.json`, `health.json`, `containers.txt` e `containers.log`, que servem de evidência textual extra.

## Prints da interface do GitHub (captura manual)

| Arquivo | Onde | O que deve aparecer |
|---|---|---|
| `01-pipeline-visao-geral.png` | Actions → execução CI/CD | O grafo com os 5 jobs verdes: Build → Testes → Imagem Docker → Deploy staging → Deploy producao |
| `02-build.png` | Job *1. Build* | O passo *Compilar e empacotar* expandido, com `BUILD SUCCESS` |
| `03-testes.png` | Página da execução (Summary) | A tabela **Testes automatizados**: unitários e integração, total aprovado e cobertura JaCoCo |
| `04-imagem-ghcr.png` | Página do repositório → *Packages* → gaiaops-esg | A imagem com as tags do commit, `latest`, `staging` e `production` |
| `05-deploy-staging.png` | Job *4. Deploy staging* | O passo *Smoke test* expandido, com **8/8 verificacoes OK** |
| `06-aprovacao-producao.png` | Execução, enquanto aguarda | O aviso *Waiting for review* / botão **Review deployments** do environment `production` |
| `07-deploy-producao.png` | Job *5. Deploy producao* | O passo *Smoke test* expandido, com **8/8 verificacoes OK** (incluindo "Swagger desabilitado") |
| `11-environments.png` | Página do repositório → *Deployments* (ou Settings → Environments) | `staging` e `production` com o último deploy bem-sucedido |

## Opcional: execução local com Docker

| Arquivo | O que capturar |
|---|---|
| `12-docker-local.png` | Terminal com `docker compose ps` mostrando `api` e `mongo` como *healthy* |

## Gerar o PDF e o .zip de novo

```powershell
py tools\gerar_documento.py
powershell -ExecutionPolicy Bypass -File tools\gerar_pdf.ps1
powershell -ExecutionPolicy Bypass -File tools\empacotar-entrega.ps1
```
