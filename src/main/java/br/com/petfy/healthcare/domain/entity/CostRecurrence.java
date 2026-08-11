package br.com.petfy.healthcare.domain.entity;

/**
 * Se o gasto se repete.
 *
 * <b>Um valor so, e nao quatro.</b> Nao ha SEMANAL nem ANUAL porque nenhuma tela pede, e um enum
 * com valores que ninguem escreve e um convite para alguem escrever sem pensar — e para a tela
 * oferecer uma escolha que nao muda nada.
 *
 * O nulo e o normal: a consulta de ontem aconteceu uma vez.
 */
public enum CostRecurrence {

    /** "Dura cerca de um mes" — a caixinha que transforma compra avulsa em custo previsivel. */
    MENSAL

}
