package br.com.petfy.healthcare.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * O fim da linha do tempo de um animal (Tela 33).
 *
 * <b>A chave primaria e o proprio animal, e nao um id gerado.</b> Morre-se uma vez, e a PK
 * garante isso sem CHECK nenhum: uma segunda tentativa de encerrar bate em violacao de chave
 * antes de qualquer regra de servico.
 *
 * <b>Ela existe como tabela, e nao como cinco colunas anulaveis em {@link Animal},</b> porque
 * colunas soltas nao conseguem prometer a coisa mais simples sobre este fato: que se ha data ha
 * tambem quem a registrou. Aqui a linha e um obito inteiro ou nao existe.
 *
 * <b>Encerrar nao e apagar.</b> O produto ja tinha {@code DELETE /animals/{id}} — que destroi a
 * carteira, o prontuario, o peso e os arquivos no disco — e nao tinha nada entre isso e nada.
 * Oferecer a destruicao de sete anos de registro a quem acabou de perder o animal e chamar isso
 * de encerramento era o buraco que esta entidade fecha.
 */
@Entity
@Table(name = "animal_deaths")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnimalDeath {

    @Id
    @Column(name = "animal_id", updatable = false, nullable = false)
    private UUID animalId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "animal_id")
    private Animal animal;

    /**
     * A data que o tutor sabe, e nao a que o servidor viu.
     *
     * E o unico campo obrigatorio da tela, e e o que faz "2019 — 2026" existir na ficha fechada.
     * A recusa a data futura mora no servico: {@code CURRENT_DATE} nao e IMMUTABLE, e o Postgres
     * nao aceita a expressao dentro de um CHECK.
     */
    @Column(name = "deceased_on", nullable = false)
    private LocalDate deceasedOn;

    /**
     * "Em casa", "na clinica", "na estrada".
     *
     * Opcional, e o produto nao insiste: quem preenche este formulario acabou de perder o animal,
     * e barrar o encerramento por um detalhe que nao muda o registro seria o oposto do que a tela
     * existe para fazer.
     */
    @Column(length = 120)
    private String place;

    /**
     * O que o tutor quis dizer, se quis.
     *
     * <b>Nao e uma {@link Observation}</b>, e a distincao e a que o produto ja faz em toda parte:
     * observacao e o que alguem VIU do animal, cai no escopo {@code OBSERVACOES} e serve de
     * evidencia a quem diagnostica. Uma despedida nao e achado clinico — e naquele escopo ela
     * seria entregue a toda creche que tem observacoes concedidas.
     */
    @Column(name = "farewell_note", columnDefinition = "text")
    private String farewellNote;

    /**
     * Quem encerrou, que e sempre quem respondia pelo animal.
     *
     * "Fechar a linha do tempo e do Marcelo, e nao pode acontecer sem ele. Ninguem deve descobrir
     * que perdeu o animal por uma notificacao do sistema." A veterinaria que atendeu na ultima
     * noite registra o obito como ato clinico dela, o que e outro registro e outro fluxo.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by_person_id", nullable = false)
    private Person recordedBy;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

}
