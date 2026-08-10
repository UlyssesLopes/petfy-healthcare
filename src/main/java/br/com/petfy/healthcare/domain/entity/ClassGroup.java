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
 * A turma: um grupo com vagas, dentro de uma organizacao.
 *
 * <b>Vaga e limite do grupo, e nao do animal</b> — "Turma Tarde · 12 de 15 vagas" e propriedade
 * da turma, e e por isso que ela e uma entidade e nao um campo da matricula.
 *
 * A capacidade nula e "sem limite declarado", e existe porque o desenho da Tela 17 poe
 * "Hospedagem" como uma aba ao lado das turmas: hospedagem costuma nao ter numero fixo, e forcar
 * um faria a creche inventar 999.
 */
@Entity
@Table(name = "class_groups")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassGroup {

    @Id
    @GeneratedValue
    @Column(name = "class_group_id", updatable = false, nullable = false)
    private UUID classGroupId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, length = 120)
    private String name;

    private Integer capacity;

    /** Turma encerrada nao apaga: as matriculas dela sao historia do animal. */
    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "creation_date", nullable = false)
    private LocalDateTime creationDate;

    public boolean estaVigente() {
        return endedAt == null;
    }
}
