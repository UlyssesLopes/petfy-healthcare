package br.com.petfy.healthcare;

import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.Custody;
import br.com.petfy.healthcare.domain.entity.CustodyNature;
import br.com.petfy.healthcare.domain.entity.Person;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Monta as custodias de um animal nos testes.
 *
 * Substituiu o {@code PetTutores}, que montava vinculos de tutor. A troca nao e de
 * nome: aquele helper tinha um {@code titularE(titular, papel, segundo)} porque um
 * co-tutor era uma linha da mesma tabela do titular. Depois do P2b nao e - quem
 * recebeu acesso e uma concessao, e um teste que precise dela monta um Grant, nao
 * uma custodia. Manter o metodo antigo faria os testes continuarem afirmando que as
 * duas coisas sao a mesma.
 */
public final class Custodias {

    private Custodias() {
    }

    /** Uma custodia em curso: quem responde pelo animal. */
    public static List<Custody> titular(Person person) {
        List<Custody> custodias = new ArrayList<>();
        custodias.add(emCurso(person));
        return custodias;
    }

    public static Custody emCurso(Person person) {
        return Custody.builder()
                .custodyId(UUID.randomUUID())
                .holderPerson(person)
                .nature(CustodyNature.DEFINITIVA)
                .startedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Uma custodia ja encerrada, para os testes que leem a historia do animal.
     *
     * Motivo obrigatorio: o banco recusa {@code ended_at} sem {@code end_reason}, e um
     * helper que produzisse esse estado deixaria o teste passar em mock e falhar
     * contra Postgres.
     */
    public static Custody encerrada(Person person, br.com.petfy.healthcare.domain.entity.CustodyEndReason motivo) {
        Custody custodia = emCurso(person);
        custodia.setEndedAt(LocalDateTime.now());
        custodia.setEndReason(motivo);
        return custodia;
    }

    /** Liga as custodias ao animal depois que ele existe, fechando os dois lados. */
    public static Animal comCustodias(Animal animal, List<Custody> custodias) {
        custodias.forEach(c -> c.setAnimal(animal));
        animal.setCustodies(custodias);
        return animal;
    }

}
