package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.AnimalYearDTO;

import java.time.LocalDate;
import java.util.UUID;

/** O ano do animal (Tela 48). */
public interface AnimalYearService {

    /**
     * O resumo do ano que termina em {@code ate}.
     *
     * <b>A data e um parametro, e nao "hoje" fixo</b>, por duas razoes. O desenho diz que o resumo
     * chega "uma vez por ano, no aniversario estimado do animal — nao em dezembro, junto com todos os
     * outros resumos do mundo", entao a janela nao e o ano civil. E quem abre o resumo do ano passado
     * em marco precisa ver o ano passado, e nao os doze meses que terminam hoje.
     */
    AnimalYearDTO resumo(UUID animalId, LocalDate ate);

}
