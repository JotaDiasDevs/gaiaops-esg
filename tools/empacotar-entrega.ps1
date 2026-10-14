<#
    =============================================================================
    GaiaOps ESG - FIAP | Navegando pelo mundo DevOps
    tools/empacotar-entrega.ps1

    Monta entrega/GaiaOps-ESG-DevOps.zip com a estrutura pedida no enunciado:

        gaiaops-esg/
        |-- Dockerfile
        |-- docker-compose.yml
        |-- src/
        |-- README.md
        |-- .github/workflows/
        `-- ... (deploy/, mongo/, docs/, tools/, .env.example, pom.xml, mvnw)

    Ficam de fora: target/, .git/, IDEs, evidencias locais e QUALQUER arquivo
    de segredo (.env e deploy/.segredos-*.env).

    O zip e criado com o tar.exe nativo do Windows: o Compress-Archive do
    PowerShell 5.1 grava caminhos com "\", que aparecem quebrados no Linux/macOS.

    Uso:  powershell -ExecutionPolicy Bypass -File tools\empacotar-entrega.ps1
    =========================================================================== #>

$ErrorActionPreference = "Stop"

$raiz = Split-Path -Parent $PSScriptRoot
$pastaEntrega = Join-Path $raiz "entrega"
$nomeZip = "GaiaOps-ESG-DevOps.zip"
$destinoZip = Join-Path $pastaEntrega $nomeZip
$temporario = Join-Path $env:TEMP "gaiaops_devops_entrega"
$pastaProjeto = Join-Path $temporario "gaiaops-esg"

Write-Host "Empacotando a entrega..." -ForegroundColor Cyan

if (Test-Path $temporario) { Remove-Item $temporario -Recurse -Force }
New-Item -ItemType Directory -Path $pastaProjeto | Out-Null
New-Item -ItemType Directory -Path $pastaEntrega -Force | Out-Null

# robocopy: copia a arvore excluindo pastas e arquivos que nao entram na entrega.
$pastasExcluidas = @("target", ".git", ".idea", ".vscode", "entrega", "evidencias", "__pycache__")
$arquivosExcluidos = @(".env", ".segredos-*.env", "*.iml", "~$*")
robocopy $raiz $pastaProjeto /E /NFL /NDL /NJH /NJS /NP /XD $pastasExcluidas /XF $arquivosExcluidos | Out-Null
if ($LASTEXITCODE -ge 8) { throw "robocopy falhou com codigo $LASTEXITCODE" }

# Conferencia de seguranca: nenhum segredo pode ter entrado no pacote.
$vazamentos = Get-ChildItem $pastaProjeto -Recurse -File -Force |
    Where-Object { $_.Name -eq ".env" -or $_.Name -like ".segredos-*" }
if ($vazamentos) { throw "Arquivo de segredo encontrado no pacote: $($vazamentos.FullName)" }

if (Test-Path $destinoZip) { Remove-Item $destinoZip -Force }
& "$env:SystemRoot\System32\tar.exe" -a -c -f $destinoZip -C $temporario "gaiaops-esg"
if ($LASTEXITCODE -ne 0) { throw "tar falhou com codigo $LASTEXITCODE" }

Remove-Item $temporario -Recurse -Force

# O PDF tambem vai separado, para o upload da documentacao.
$pdf = Join-Path $raiz "docs\GaiaOps-ESG-DevOps.pdf"
if (Test-Path $pdf) { Copy-Item $pdf -Destination $pastaEntrega -Force }

$mb = [math]::Round((Get-Item $destinoZip).Length / 1MB, 2)
Write-Host "Pacote gerado: entrega\$nomeZip ($mb MB)" -ForegroundColor Green
if (Test-Path $pdf) {
    Write-Host "Documentacao:  entrega\GaiaOps-ESG-DevOps.pdf" -ForegroundColor Green
} else {
    Write-Host "PDF ainda nao gerado: rode tools\gerar_pdf.ps1 antes de empacotar." -ForegroundColor Yellow
}
