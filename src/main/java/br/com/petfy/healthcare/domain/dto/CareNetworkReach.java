package br.com.petfy.healthcare.domain.dto;

/**
 * Por que alguem alcanca um animal.
 *
 * <b>Custodia e acesso sao coisas diferentes, e a Fase 6 existiu para separa-las.</b> Quem
 * detem custodia <i>responde</i> pelo animal; quem tem concessao <i>alcanca</i> parte dele,
 * com escopo e as vezes com prazo. Colapsar os dois num campo de papel foi o modelo que a
 * Fase 6 desmontou, e a tela nao pode reintroduzi-lo.
 */
public enum CareNetworkReach {

    /** Responde pelo animal (3.4). Alcanca tudo, e ninguem concedeu isso a ela. */
    CUSTODIA,

    /** Recebeu acesso delimitado (3.5), com escopo e as vezes com prazo. */
    CONCESSAO

}
