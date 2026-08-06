package br.com.petfy.healthcare.domain.entity;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Quem pode ler ou escrever no registro de um animal, no que, e ate quando.
 *
 * <b>Acesso e custodia sao coisas diferentes, e ate o P2 eram a mesma.</b> O papel
 * no PetTutor decidia as duas: quem responde pelo animal e quem alcanca o animal.
 * Misturar faz "conceder acesso a clinica" parecer parente de "transferir
 * titularidade", quando sao opostos - e obrigaria a creche a virar co-tutora de 40
 * animais para poder ver a carteira deles.
 *
 * <table>
 *   <tr><th></th><th>Custodia</th><th>Acesso</th></tr>
 *   <tr><td>Responde pelo animal</td><td>Sim</td><td>Nao</td></tr>
 *   <tr><td>Quantos ao mesmo tempo</td><td>Poucos</td><td>Muitos</td></tr>
 *   <tr><td>Termina</td><td>So com sucessor</td><td>A qualquer momento</td></tr>
 * </table>
 *
 * <b>Tres tipos de beneficiario, um so por linha</b> - garantido por CHECK no
 * banco, nao so por regra de servico:
 * <ul>
 *   <li>uma <b>pessoa</b>: o co-tutor, o filho adulto, o cuidador, o vet autonomo;</li>
 *   <li>uma <b>clinica</b>: quem atende hoje pode nao ser quem atende no retorno,
 *       entao o tutor autoriza a organizacao, e nao um profissional;</li>
 *   <li>um <b>token</b>: o link de carteira, para quem nao tem conta. E o cartao
 *       de emergencia da decisao 13, agora com escopo.</li>
 * </ul>
 */
@Entity
@Table(name = "grants")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Grant {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(name = "grant_id", updatable = false, nullable = false)
    private UUID grantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animal_id", nullable = false)
    private Animal animal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grantee_person_id")
    private Person granteePerson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grantee_clinic_id")
    private Clinic granteeClinic;

    /**
     * Hash do token do link, quando o beneficiario nao tem conta.
     *
     * Guardado como hash pelo mesmo motivo do AnimalShare que ele substitui: e a
     * unica credencial do link, entao um vazamento do banco nao pode entregar as
     * carteiras ativas. A consequencia de produto continua a mesma - o token so
     * aparece uma vez, na criacao.
     */
    @Column(name = "token_hash", unique = true)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GrantLevel level;

    /**
     * O quanto do animal este acesso alcanca. Nunca vazio: um acesso sem escopo
     * nao alcanca nada, e uma linha assim seria so uma forma silenciosa de negar.
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "grant_scopes", joinColumns = @JoinColumn(name = "grant_id"))
    @Column(name = "scope", nullable = false, length = 24)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<GrantScope> scopes = new LinkedHashSet<>();

    /** Quem concedeu. Todo acesso nasce de alguem que responde pelo animal. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "granted_by_person_id")
    private Person grantedBy;

    @Column(name = "granted_at", nullable = false)
    private LocalDateTime grantedAt;

    /** Nulo em acesso sem prazo - o do co-tutor, por exemplo. */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    public boolean estaVigente(LocalDateTime agora) {
        return revokedAt == null && (expiresAt == null || expiresAt.isAfter(agora));
    }

    public boolean alcanca(GrantScope escopo) {
        return scopes != null && scopes.contains(escopo);
    }

}
