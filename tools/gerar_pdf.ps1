<#
    =============================================================================
    GaiaOps ESG - FIAP | Navegando pelo mundo DevOps
    tools/gerar_pdf.ps1

    Converte docs/GaiaOps-ESG-DevOps.docx em PDF com o LibreOffice headless.
    (Gere o .docx antes:  py tools\gerar_documento.py)

    Notas de ambiente (herdadas da Fase 5):
      - soffice.com e a variante de console: espera a conversao terminar;
      - o stderr do LibreOffice NAO e redirecionado: no PowerShell 5.1 isso
        geraria NativeCommandError mesmo com sucesso.

    Uso:  powershell -ExecutionPolicy Bypass -File tools\gerar_pdf.ps1
    =========================================================================== #>

$ErrorActionPreference = "Stop"

$raiz = Split-Path -Parent $PSScriptRoot
$docx = Join-Path $raiz "docs\GaiaOps-ESG-DevOps.docx"
$pdf = Join-Path $raiz "docs\GaiaOps-ESG-DevOps.pdf"

if (-not (Test-Path $docx)) {
    throw "Arquivo nao encontrado: $docx`nGere o documento primeiro:  py tools\gerar_documento.py"
}

$soffice = $null
foreach ($c in @("$env:ProgramFiles\LibreOffice\program\soffice.com",
                 "${env:ProgramFiles(x86)}\LibreOffice\program\soffice.com",
                 "$env:ProgramFiles\LibreOffice\program\soffice.exe")) {
    if (Test-Path $c) { $soffice = $c; break }
}
if (-not $soffice) {
    Write-Host "LibreOffice nao encontrado. Instale com:" -ForegroundColor Yellow
    Write-Host "  winget install TheDocumentFoundation.LibreOffice --source winget" -ForegroundColor Yellow
    Write-Host "Ou abra o .docx no Word e use Arquivo > Exportar > PDF." -ForegroundColor Yellow
    exit 1
}

Write-Host "Convertendo com LibreOffice headless..."
if (Test-Path $pdf) { Remove-Item $pdf -Force }

$preferencia = $ErrorActionPreference
$ErrorActionPreference = "Continue"
try {
    & $soffice --headless --norestore --convert-to "pdf:writer_pdf_Export" --outdir (Join-Path $raiz "docs") $docx
}
finally {
    $ErrorActionPreference = $preferencia
}

if (Test-Path $pdf) {
    $mb = [math]::Round((Get-Item $pdf).Length / 1MB, 2)
    Write-Host "PDF gerado: docs\GaiaOps-ESG-DevOps.pdf ($mb MB)" -ForegroundColor Green
} else {
    Write-Host "A conversao nao produziu o PDF. Alternativa: exporte o .docx pelo Word." -ForegroundColor Red
    exit 1
}
