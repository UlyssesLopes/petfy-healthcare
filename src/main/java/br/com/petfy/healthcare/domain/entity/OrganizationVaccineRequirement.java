package br.com.petfy.healthcare.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * O que esta organizacao exige da carteira de quem entra.
 *
 * <b>A frase que esta entidade sustenta e da Tela 10:</b> "Gripe canina — sem registro. NAO E
 * EXIGIDA pela Creche Quintal". Sem ela, o produto teria de escolher entre exigir o catalogo
 * inteiro — e barrar animal saudavel por uma vacina que aquela creche nao pede — ou nao exigir
 * nada, e deixar entrar animal com antirrabica vencida, que e o risco que a creche corre por lei.
 *
 * E por ORGANIZACAO, e nao por turma: a creche exige o que exige de quem entra na porta dela, e
 * nao um conjunto diferente por horario.
 */
@Entity
@Table(name = "organization_vaccine_requirements")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationVaccineRequirement {

    @Id
    @GeneratedValue
    @Column(name = "requirement_id", updatable = false, nullable = false)
    private UUID requirementId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vaccine_catalog_id", nullable = false)
    private VaccineCatalog vaccineCatalog;

    @Column(name = "creation_date", nullable = false)
    private LocalDateTime creationDate;
}
