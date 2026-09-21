package com.iah.nutrition.dto;

import java.math.BigDecimal;

/**
 * Header row for the client formula library ({@code CLIENT_FORMULAS_HDR} / {@code CLTFMHDR}).
 */
public record ClientFormulasHdr(
        Long clientId,
        Long formulaId,
        String formulaDescription,
        BigDecimal lastPrice,
        Long animalId,
        String optimizationTechnique
) {
}
