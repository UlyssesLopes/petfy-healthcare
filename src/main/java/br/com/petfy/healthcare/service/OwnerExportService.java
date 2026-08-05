package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.OwnerExportDTO;

/**
 * Portabilidade: tudo que o Petfy guarda sobre o titular, num documento.
 *
 * Irmao da exclusao que o passo 10 entregou. Sair do sistema sem poder levar o historico
 * de saude do proprio animal deixa o tutor preso ao produto por refem, e nao por escolha.
 */
public interface OwnerExportService {

    OwnerExportDTO exportarDoAutenticado();

}
