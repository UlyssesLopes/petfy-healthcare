package br.com.petfy.healthcare.domain.entity;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Um coletivo que atua sobre animais, com membros e capacidades.
 *
 * <b>Substituiu a Clinic, e a diferenca nao e de nome.</b> Clinica, creche e abrigo
 * nao sao tres entidades: sao a mesma exercendo conjuntos diferentes de capacidades.
 * Modelar como tipo obrigaria a escolher um e mentir sobre o resto - ONG com clinica
 * propria faz as tres, creche que hospeda faz duas.
 *
 * O que era especifico de clinica continua aqui, porque continua sendo verdade de
 * qualquer organizacao que atende: nome, CNPJ, endereco, contato. O que era
 * <i>implicito</i> - "isto e uma clinica, entao registra ato clinico" - virou
 * explicito em {@link OrganizationCapability}.
 */
@Entity
@Table(name = "organizations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Organization {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator")
    @Column(updatable = false, nullable = false)
    private UUID organizationId;

    private String name;

    private String ownerVetName;

    private String phone;

    private String email;

    private String cnpj;

    private String address;

    private String city;

    private String state;

    private String cep;

    private String description;

    /**
     * O que esta organizacao pode fazer.
     *
     * Nunca vazio: organizacao sem capacidade nenhuma nao alcanca nada e nao serve
     * para nada - uma linha assim seria so uma forma silenciosa de desativar. Quem
     * cria escolhe; sem escolha, o servico aplica o conjunto de uma clinica, que e o
     * que toda organizacao existente era.
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "organization_capabilities",
            joinColumns = @JoinColumn(name = "organization_id"))
    @Column(name = "capability", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<OrganizationCapability> capabilities = new LinkedHashSet<>();

    private LocalDateTime creationDate;

    private LocalDateTime updateDate;

    public boolean pode(OrganizationCapability capacidade) {
        return capabilities != null && capabilities.contains(capacidade);
    }

}
