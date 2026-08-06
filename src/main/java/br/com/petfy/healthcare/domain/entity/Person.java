package br.com.petfy.healthcare.domain.entity;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "persons")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Person {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(updatable = false, nullable = false)
    private UUID personId;

    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private String phone;

    private String address;

    private LocalDateTime creationDate;

    private LocalDateTime updateDate;

    /**
     * Instante da ultima troca de senha. Nulo em quem nunca trocou, o que
     * significa que nao ha token a invalidar.
     */
    private LocalDateTime passwordChangedAt;

    /**
     * Instante em que o tutor confirmou o proprio e-mail. Nulo suspende as
     * notificacoes, mas nao o login - ver V12__email_verification.sql.
     */
    private LocalDateTime emailVerifiedAt;

    /**
     * A clinica em que a pessoa atua. <b>Ponte transitoria, e ela morre no P3.</b>
     *
     * O lugar certo disto e {@code Membership}, porque uma pessoa atua em N
     * organizacoes - vet em duas clinicas, voluntario de abrigo, dono de creche
     * que tambem e tutor. Enquanto Membership nao existe, a coluna fica aqui em
     * vez de o P1 arrastar a Fase 3 inteira junto.
     *
     * <b>O que ja muda por ela ser nullable:</b> o veterinario autonomo deixa de
     * ser impossivel por constraint. Antes era {@code nullable = false} em
     * {@code vets}, entao atendimento domiciliar obrigava a inventar uma clinica.
     * Que o cadastro ainda exija convite ou clinica nova e outro assunto, e ele e
     * do P3.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id")
    private Clinic clinic;

    public boolean podeReceberNotificacao() {
        return emailVerifiedAt != null;
    }

    /** Atua em nome de uma organizacao, e nao por si. E o contexto da secao 3.2. */
    public boolean atuaPorClinica() {
        return clinic != null;
    }

}
