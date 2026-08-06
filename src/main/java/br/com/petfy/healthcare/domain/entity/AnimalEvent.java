package br.com.petfy.healthcare.domain.entity;

import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.Getter;
import lombok.Setter;

/**
 * O nucleo comum de tudo que acontece com um animal: quem registrou, e em nome de
 * quem.
 *
 * <b>MappedSuperclass, e nao tabela.</b> O produto recusa explicitamente a tabela
 * generica com campo JSON: ela destruiria o que o backend ja sabe - que vacina tem
 * validade e proxima dose, que condicao tem gravidade e encerramento, que peso e
 * serie. O nucleo permite ordenar, filtrar e auditar tudo junto; a especializacao
 * preserva o valor clinico. Aqui o nucleo entra sem cirurgia de chave primaria.
 *
 * <b>O que deliberadamente NAO esta aqui: o instante do fato.</b> Cada evento ja tem
 * o seu - {@code applicationDate}, {@code eventDate}, {@code measuredAt}, {@code
 * since} -, e acrescentar um {@code occurredAt} ao lado criaria duas respostas para
 * "quando aconteceu". O dia em que divergissem, a linha do tempo mentiria sobre a
 * ordem dos fatos. Quem uniformiza e a view {@code animal_timeline}, que le a coluna
 * de cada tabela e a apresenta com um nome so.
 *
 * <b>E o que tambem nao esta: a classificacao de dado de saude.</b> Ela e uma
 * propriedade do <i>tipo</i> de evento, e nao da linha - ato clinico sempre e,
 * observacao quase sempre e, recado e foto nao sao. Guardar por linha seria abrir a
 * porta para duas vacinas discordarem sobre serem dado de saude. A view a deriva do
 * tipo.
 */
@MappedSuperclass
@SuperBuilder
@NoArgsConstructor
@Getter
@Setter
public abstract class AnimalEvent {

    /**
     * Quem registrou. <b>Nunca editavel</b> - e o que separa um registro que um
     * veterinario aceita de um caderno digital.
     *
     * Nulo nos eventos gravados antes deste passo. A migration nao inventa autor: nao
     * havia coluna, ninguem foi registrado, e preencher com o titular atual afirmaria
     * um fato que nao aconteceu. E o mesmo raciocinio do consentimento, que a V16
     * recusou-se a fabricar.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by_person_id")
    private Person recordedBy;

    /**
     * Em nome de qual organizacao, quando houve uma.
     *
     * Nulo quando a pessoa agiu por si - o tutor lancando a vacina da carteirinha de
     * papel, ou o veterinario autonomo. "A Ana registrou" e "a Ana, pela Clinica
     * Norte, registrou" sao fatos diferentes, e so o segundo carrega responsabilidade
     * institucional.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

}
