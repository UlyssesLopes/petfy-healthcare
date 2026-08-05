package br.com.petfy.healthcare.domain.entity;

/**
 * Alergia ou condicao cronica.
 *
 * Duas coisas na mesma tabela porque tem a mesma forma - o que o animal tem, desde
 * quando, se ainda vale - e a mesma razao de existir: aparecer em destaque, e nao
 * enterrado numa descricao de atendimento.
 */
public enum PetHealthConditionKind {

    /**
     * Reacao a substancia. E a que mais precisa de destaque: alergia a anestesico perdida
     * no meio de um texto corrido e o tipo de informacao que so se descobre que faltava
     * depois de um procedimento.
     */
    ALERGIA,

    /**
     * Diabetes, cardiopatia, epilepsia, insuficiencia renal. Muda o que e normal para
     * aquele animal, entao muda a leitura de todo o resto do prontuario.
     */
    CONDICAO_CRONICA

}
