package br.com.petfy.healthcare.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * O caso que passa adiante: uma clinica indica o especialista, e o tutor decide (Tela 45).
 *
 * <b>"Encaminhar e voce indicando o caminho; conceder acesso continua sendo dele, como sempre
 * foi."</b> Esta entidade nao concede nada — ela e o pedido. O que o aceite produz e um
 * {@link Grant} comum, com prazo, concedido pelo tutor e revogavel por ele; o {@link #grantAcesso}
 * apenas registra qual concessao nasceu daqui.
 *
 * <b>Tres partes decidem em momentos diferentes</b>, e e isso que nenhuma entidade existente
 * modelava:
 * <ul>
 *   <li><b>quem encaminha</b> escolhe para quem, por que, e o que vai junto;</li>
 *   <li><b>o tutor</b> autoriza ou recusa — e so ele, porque o que se decide e acesso ao
 *       prontuario;</li>
 *   <li><b>o especialista</b> recebe, e nao e consultado em momento nenhum.</li>
 * </ul>
 *
 * <b>Nao e o {@link PetTutorInvite} com outro nome.</b> No convite quem decide e o BENEFICIARIO — o
 * conjuge aceita entrar no animal, e o token no e-mail dele e a credencial disso. Aqui quem decide
 * e um terceiro, e quem se beneficia so descobre depois de a decisao estar tomada.
 *
 * <b>Nem e o {@link GroupApproval}.</b> Lá quem decide e indeterminado de proposito: qualquer outra
 * pessoa do grupo serve, e essa indeterminacao e o mecanismo. Aqui e o oposto — so quem responde
 * pelo animal autoriza, e ninguem mais.
 */
@Entity
@Table(name = "referrals")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Referral {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "referral_id", updatable = false, nullable = false)
    private UUID referralId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animal_id", nullable = false)
    private Animal animal;

    /**
     * Quem encaminha. A pessoa, e nao a clinica: encaminhar e um ato profissional com autoria, e
     * "a Clinica Vet Norte encaminhou" esconderia quem examinou o animal.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referred_by_person_id", nullable = false)
    private Person referredBy;

    /** Em nome de que clinica. Nulo no veterinario autonomo, que encaminha tambem. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_organization_id")
    private Organization fromOrganization;

    /**
     * O especialista. <b>Pessoa, e nao organizacao</b> — e e a diferenca mais importante entre isto
     * e a concessao a uma clinica.
     *
     * Lá o beneficiario e a organizacao de proposito: "quem atende hoje pode nao ser quem atende no
     * retorno". Encaminhar e o caso oposto, e o desenho e explicito — "ele ja registrou o raio-X do
     * Code em 2023". Encaminha-se para o Roberto porque e o Roberto; mandar o caso para a Clinica
     * Anhangabau entregaria a ortopedia a quem estiver na recepcao.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_person_id", nullable = false)
    private Person toPerson;

    /**
     * O porque, escrito por quem encaminha. <b>Obrigatorio</b>, e o unico texto que e.
     *
     * Duas pessoas o leem com perguntas diferentes: o tutor decide autorizar com base nele, e o
     * especialista descobre por ele o que esta sendo perguntado. Sem motivo, o tutor autorizaria o
     * desconhecido e o especialista receberia um prontuario sem pergunta — que e a foto de WhatsApp
     * que esta tela existe para acabar.
     */
    @Column(columnDefinition = "text", nullable = false)
    private String reason;

    /**
     * O quanto do animal o especialista alcanca, se autorizado.
     *
     * <b>Por escopo, e nao por evento</b>, e o compromisso e consciente. O desenho seleciona eventos
     * — "o raio-X de 2023", "4 observacoes da creche entre 02/06 e 05/08" — e a concessao deste
     * produto so sabe conceder por tipo. Um recorte por evento tambem mentiria com facilidade: as
     * quatro observacoes escolhidas hoje nao dizem nada sobre a quinta, escrita amanha pela mesma
     * creche sobre o mesmo problema, e o especialista tratando o caso nao a veria.
     *
     * Nunca vazio, pela mesma razao do {@link Grant#getScopes()}: um encaminhamento que nao leva
     * nada seria so uma forma silenciosa de nao encaminhar.
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "referral_scopes", joinColumns = @JoinColumn(name = "referral_id"))
    @Column(name = "scope", nullable = false, length = 24)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<GrantScope> scopes = new LinkedHashSet<>();

    /**
     * Quantos dias o acesso vale, se autorizado. 90 e o que a tela promete.
     *
     * <b>No pedido, e nao constante no servico</b>, porque o tutor precisa ver o prazo ANTES de
     * autorizar — "o acesso do Roberto vale 90 dias e depois fecha sozinho" e parte do que ele esta
     * decidindo. Uma constante faria a tela de decisao afirmar um numero que nada no pedido
     * sustenta, e mudar o default amanha reescreveria o passado.
     */
    @Column(name = "access_days", nullable = false)
    private Integer accessDays;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ReferralStatus status;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by_person_id")
    private Person decidedBy;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    /**
     * A concessao que o aceite produziu, e a prova de que o encaminhamento nao concede.
     *
     * Ela aponta para a linha em {@code grants} que o tutor criou ao autorizar — com
     * {@code grantedBy} sendo ele, revogavel por ele a qualquer momento, aparecendo na lista de
     * acessos do animal como qualquer outra.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grant_id")
    private Grant grantAcesso;

    public boolean estaPendente() {
        return status == ReferralStatus.PENDENTE;
    }

}
