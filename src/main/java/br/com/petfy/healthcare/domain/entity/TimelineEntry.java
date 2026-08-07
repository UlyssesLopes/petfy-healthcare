package br.com.petfy.healthcare.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Uma entrada da linha do tempo de um animal.
 *
 * <b>Mapeia uma view, e nao uma tabela.</b> {@code @Immutable} nao e zelo: o Hibernate
 * passa a recusar insert, update e delete nesta entidade, e a linha do tempo nao tem
 * escrita propria - quem escreve e cada evento na sua tabela. Sem isso, uma escrita
 * acidental aqui falharia no banco em vez de na compilacao.
 *
 * <b>Por que uma view e nao uma tabela-indice.</b> A tabela-indice precisaria ser
 * alimentada por quem grava cada evento, e uma escrita nova que esquecesse de inserir
 * o indice deixaria o evento fora da linha do tempo <i>em silencio</i>. Divergencia
 * silenciosa foi a familia dos seis bugs da Fase 4. Uma view nao tem como divergir,
 * porque nao existe segunda escrita.
 *
 * <b>A linha do tempo e do backend, e nao da tela.</b> O que entra, o que quem le pode
 * ver e como se ordena sao regra de dominio - a alternativa era o cliente chamar seis
 * endpoints e ordenar em memoria, e nesse arranjo cada cliente novo reimplementaria a
 * regra do seu jeito.
 */
@Entity
@Immutable
@Table(name = "animal_timeline")
@Getter
@NoArgsConstructor
public class TimelineEntry {

    /**
     * O id do evento na tabela de origem.
     *
     * Serve de chave primaria porque UUID nao colide entre tabelas - dois eventos de
     * tipos diferentes nunca compartilham id. Nao ha coluna de identidade propria na
     * view, e inventar uma exigiria a tabela-indice que este desenho evita.
     */
    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private TimelineEventType eventType;

    @Column(name = "animal_id", nullable = false)
    private UUID animalId;

    /**
     * Quando aconteceu. <b>E por este campo que a linha do tempo ordena</b>, e nao pelo
     * instante do registro: o tutor cadastra hoje a vacina de 2019 da carteirinha de
     * papel, e ela aparece em 2019. Linha do tempo ordenada por data de digitacao
     * mente.
     */
    @Column(name = "occurred_at")
    private LocalDateTime occurredAt;

    /**
     * Quando foi registrado. A creche registra as 18h o que viu as 9h, e a diferenca
     * entre os dois instantes e informacao - nao ruido.
     */
    @Column(name = "recorded_at")
    private LocalDateTime recordedAt;

    /** Nulo nos eventos anteriores ao P4: nao havia coluna, e a migration nao inventa autor. */
    @Column(name = "recorded_by_person_id")
    private UUID recordedByPersonId;

    /** Nulo quando a pessoa agiu por si. */
    @Column(name = "organization_id")
    private UUID organizationId;

    /**
     * Se este evento e dado de saude para efeito de LGPD.
     *
     * Derivado do tipo pela view, e nao guardado por linha - senao duas vacinas
     * poderiam discordar. Hoje todos os seis tipos sao; quando o conteudo nao clinico
     * do Horizonte 3 chegar - recado, foto -, ele entra como falso.
     */
    @Column(name = "is_health_data", nullable = false)
    private boolean healthData;

    /** Uma linha de texto que identifica o evento. O detalhe mora no recurso proprio. */
    @Column(name = "summary")
    private String summary;

    /**
     * O nome de quem registrou, resolvido pela view.
     *
     * <b>Vem daqui e nao de uma consulta por entrada.</b> O servico fazia
     * {@code personRepository.findById()} para cada linha, entao uma pagina de vinte
     * eventos custava vinte consultas - e a linha do tempo e a leitura que mais cresce
     * neste produto. Nulo nos eventos anteriores ao P4, que nao tem autor.
     */
    @Column(name = "recorded_by_name")
    private String recordedByName;

    /**
     * Em nome de que organizacao o registro foi feito, quando houve uma.
     *
     * "A Ana, pela Clinica Norte, registrou" carrega responsabilidade institucional, e
     * "a Ana registrou" nao (PRODUTO 3.2). Nulo quando quem registrou agia por si.
     */
    @Column(name = "organization_name")
    private String organizationName;

    /** A credencial de quem registrou, ja formatada como {@code CRMV-SP 12345}. */
    @Column(name = "credential_label")
    private String credentialLabel;

    /**
     * O estado da credencial.
     *
     * <b>Vai junto do rotulo de proposito.</b> A secao 5.2 do DESIGN cobra que CRMV
     * apenas informado apareca <i>como informado</i>, sem selo de verificado que o
     * produto nao pode dar (5.10) - e sem este campo a tela ou omite a credencial ou
     * mente sobre ela.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "credential_status")
    private CredentialStatus credentialStatus;

    /**
     * Quantas vezes este evento foi corrigido.
     *
     * Contagem, e nao a cadeia: a tela precisa saber <i>que</i> houve sucessao para
     * marcar o evento (DESIGN 5.2), e o valor anterior continua nas rotas de correcao.
     * Zero nos tipos que nao admitem correcao.
     */
    @Column(name = "correction_count", nullable = false)
    private long correctionCount;

    /**
     * O peso da pesagem anterior, quando esta entrada e uma pesagem.
     *
     * <b>So o anterior, e nao a serie.</b> O grafico precisa de N pontos e quem os serve e
     * {@code GET /animals/{id}/weights}; o que a <i>entrada</i> precisa e outra coisa -
     * "12,5 kg" sozinho nao diz se e boa ou ma noticia. Com o valor anterior o cliente
     * calcula a variacao e a linha fica honesta, sem virar painel (DESIGN 5.2).
     *
     * Nulo fora de pesagem, e nulo na primeira pesagem do animal - zero diria "nao
     * variou", que e diferente de "nao ha com o que comparar".
     */
    @Column(name = "previous_weight")
    private Double previousWeight;

}
