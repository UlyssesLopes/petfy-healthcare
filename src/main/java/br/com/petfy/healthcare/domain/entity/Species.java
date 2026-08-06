package br.com.petfy.healthcare.domain.entity;

/**
 * Especie do animal. Nao e String livre porque toda associacao entre animal e catalogo
 * de vacinas depende de casar espécie exata - "cachorro" vs "canina" vs "Canis
 * familiaris" cai fora do match e a vacina errada passa despercebida.
 *
 * Sao intencionalmente poucas: o produto so trata caes e gatos hoje. Ganhar
 * suporte a outra especie e um passo deliberado - novo valor aqui, mais entrada
 * no catalogo, atualizacao da UI se existir.
 */
public enum Species {
    CANINA,
    FELINA
}
