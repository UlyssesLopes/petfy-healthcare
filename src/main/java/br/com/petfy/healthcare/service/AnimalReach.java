package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.entity.Custody;
import br.com.petfy.healthcare.domain.entity.Grant;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.repository.CustodyRepository;
import br.com.petfy.healthcare.domain.repository.GrantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * As pessoas que alcancam um animal agora: quem responde por ele, mais quem recebeu
 * acesso.
 *
 * <b>Existe como peca unica pelo mesmo motivo do AnimalAccessGuard.</b> Depois do
 * P2b a resposta vem de duas tabelas, e a pergunta e feita em quatro lugares - o
 * lembrete de vacina, o aviso de clinica, o aviso de mudanca de tutores e a listagem
 * de quem cuida. Quatro copias de "custodia mais concessao" e quatro chances de
 * alguem esquecer uma das duas, e o efeito de esquecer nao e tela quebrada: e aviso
 * sobre dado de saude indo para o conjunto errado de pessoas.
 *
 * <b>Por consulta, nunca pela colecao de {@code Animal}.</b> A colecao e lazy e em
 * geral acabou de ser mexida por quem chama - custodia encerrada, concessao criada ou
 * revogada -, entao ler dela devolveria uma lista que pode nao refletir o que foi
 * gravado. E a licao do 8c, que ja custou este bug uma vez.
 */
@Component
@RequiredArgsConstructor
public class AnimalReach {

    private final CustodyRepository custodyRepository;
    private final GrantRepository grantRepository;

    /**
     * Quem responde primeiro, quem tem acesso depois.
     *
     * A ordem nao e cosmetica: e a mesma que a ordenacao por papel produzia, e a
     * listagem de quem cuida do animal depende dela para mostrar o responsavel no
     * topo.
     */
    public List<Person> pessoas(UUID animalId) {
        List<Person> pessoas = new ArrayList<>();

        custodyRepository.findEmCurso(animalId)
                .map(Custody::getHolderPerson)
                // nulo quando quem responde e uma organizacao: um abrigo nao tem
                // caixa de e-mail de tutor, e o aviso simplesmente nao tem destino
                .filter(java.util.Objects::nonNull)
                .ifPresent(pessoas::add);

        grantRepository.findVigentesDePessoasNoAnimal(animalId, LocalDateTime.now())
                .stream()
                .map(Grant::getGranteePerson)
                .forEach(pessoas::add);

        return pessoas;
    }

    /** Alguem alcanca o animal e pode receber aviso. */
    public boolean temAlguemNotificavel(UUID animalId) {
        return pessoas(animalId).stream().anyMatch(Person::podeReceberNotificacao);
    }

}
