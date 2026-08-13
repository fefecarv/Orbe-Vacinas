package br.com.orbe.dto;

import java.math.BigDecimal;

public record AnaliseConvenio(
        BigDecimal valorBase,
        String tipoCobertura,
        BigDecimal percentualDesconto,
        BigDecimal valorCoberto,
        BigDecimal valorPaciente,
        String status,
        String mensagem
) {
}
