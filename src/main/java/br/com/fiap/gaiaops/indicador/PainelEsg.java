package br.com.fiap.gaiaops.indicador;

import java.time.Instant;
import java.util.Map;

/**
 * Painel ESG do municipio: um retrato dos tres pilares calculado sobre a base.
 * Serializado em snake_case, como o restante da API.
 */
public record PainelEsg(Instant geradoEm, Ambiental ambiental, Social social, Governanca governanca) {

    public record Ambiental(long medicoes,
                            long alertasAbertos,
                            Map<String, Long> alertasAbertosPorNivel,
                            double residuosColetadosKg,
                            double taxaDesvioDeAterroPct) {
    }

    public record Social(long programasAtivos,
                         long beneficiariosAcompanhados,
                         double orcamentoPrevistoBrl,
                         double orcamentoExecutadoBrl,
                         double execucaoOrcamentariaPct) {
    }

    /** licencasPorVencer considera os proximos 180 dias. */
    public record Governanca(long equipamentosAtivos,
                             long equipamentosTotal,
                             long licencasVencidas,
                             long licencasPorVencer,
                             long manifestacoesOuvidoria,
                             long termosAjustamento) {
    }
}
