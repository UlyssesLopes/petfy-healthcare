package br.com.petfy.healthcare.domain.entity;

/**
 * Onde o dinheiro foi (Tela 37).
 *
 * <b>Existe porque o {@link AnimalCostKind} responde outra pergunta.</b> O `kind` diz de que EVENTO
 * o valor saiu — atendimento, mensalidade, diaria, compra —, e serve para o tutor ler agrupado por
 * origem. A categoria diz em QUE o dinheiro foi gasto, e as duas divergem no caso que mais importa:
 * racao e remedio saem os dois de uma `COMPRA` do tutor, e o desenho os poe em fatias diferentes.
 *
 * <b>E a divergencia nao e detalhe: e uma frase do desenho.</b> "Saude e o menor pedaco do gasto do
 * Code — e e o UNICO QUE CRESCE SOZINHO QUANDO E ADIADO. Os outros tres sao escolha sua." Sem
 * separar alimentacao de saude, essa frase perde o sentido, e com ela a tese da Tela 38.
 *
 * <b>Nao ha categoria "TRANSPORTE", "SEGURO" nem "BRINQUEDO"</b>, e a ausencia e a mesma regra do
 * {@link CostRecurrence}: nenhuma tela pede, e um enum com valores que ninguem escreve e um convite
 * para alguem escrever sem pensar.
 */
public enum AnimalCostCategory {

    /**
     * Consulta, exame, vacina, remedio.
     *
     * <b>E a unica categoria que o produto trata diferente</b> — e nao aqui, e na Tela 38: adiar
     * saude tem custo, e adiar creche nao. Mas o dominio nao olha para este campo; quem olha e a
     * leitura.
     */
    SAUDE,

    /** Racao e o que mais o animal come. */
    ALIMENTACAO,

    /** Mensalidade e diaria: o que a creche cobra. */
    CRECHE,

    /**
     * Banho, tosa, higiene.
     *
     * <b>Nasce sem ninguem para escrever nela</b>, e isso e esperado: quem cobra banho e o petshop,
     * que e o bloco 8. Uma fatia sem valor nao aparece na Tela 37 — entao a categoria existir vazia
     * nao custa nada, e o dia em que o petshop registrar valor ela ja tem lugar.
     */
    HIGIENE,

    /** O que nao e nenhum dos quatro. O terceiro botao da Tela 42. */
    OUTRO

}
