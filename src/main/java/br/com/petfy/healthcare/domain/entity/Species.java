package br.com.petfy.healthcare.domain.entity;

/**
 * Especie do animal. Nao e String livre porque toda associacao entre animal e catalogo
 * de vacinas depende de casar espécie exata - "cachorro" vs "canina" vs "Canis
 * familiaris" cai fora do match e a vacina errada passa despercebida.
 *
 * <b>Deixou de ser so cao e gato em 2026-08-15.</b> Ate ali o produto recusava o resto, e a
 * tela de cadastro tinha um botao "Outro" desabilitado com um paragrafo explicando por que o
 * animal da pessoa nao cabia aqui. Quem tem calopsita, coelho ou jabuti tambem tem carteirinha
 * de vacina, historico e alguem que cuida - e "o registro e do animal" nao pode valer so para
 * duas especies.
 *
 * <b>Ampliar nao exigiu migration:</b> a coluna e {@code varchar(32)} sem CHECK, desde a V13.
 *
 * <b>O que muda para as especies novas, e e honesto:</b> o catalogo de vacinas e o de
 * antiparasitarios so tem entradas para CANINA e FELINA, entao um animal de outra especie nao
 * recebe sugestao nenhuma. Ele guarda o que o tutor lancar, e nao inventa protocolo que o
 * produto nao conhece - o contrario seria sugerir dose de cachorro para passaro.
 *
 * <b>OUTRA e o fim da lista, e nao o comeco.</b> Ela existe para o animal que nao cabe em
 * nenhuma das outras, e nao para poupar o trabalho de nomear as comuns: uma lista que colapsa
 * tudo em "outra" perde a unica informacao que o dono acabou de dar.
 */
public enum Species {
    CANINA,
    FELINA,
    AVE,
    ROEDORA,
    LAGOMORFA,
    REPTIL,
    EQUINA,
    OUTRA
}
