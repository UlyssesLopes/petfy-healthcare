package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.Grant;
import br.com.petfy.healthcare.domain.entity.Organization;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.repository.CustodyRepository;
import br.com.petfy.healthcare.domain.repository.GrantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Para quem ligar por causa de um animal: quem responde por ele, e as organizacoes que o atendem.
 *
 * <b>A ordem e a resposta.</b> Quem responde pelo animal vem primeiro, porque e quem pode ir
 * busca-lo; as clinicas vem depois, e sabem o caso.
 *
 * Mora fora dos dois servicos que a usam — o cartao de quem achou o animal na rua e o cartao
 * compartilhado — porque e a mesma pergunta feita por duas portas.
 */
@Component
@RequiredArgsConstructor
public class AnimalContacts {

    /** {@code kind} distingue TUTOR de ORGANIZACAO: muda o que quem liga espera do outro lado. */
    public record Contato(String name, String phone, String kind) {

        public static final String TUTOR = "TUTOR";
        public static final String ORGANIZACAO = "ORGANIZACAO";

    }

    private final CustodyRepository custodyRepository;
    private final GrantRepository grantRepository;

    public List<Contato> de(Animal animal) {
        List<Contato> contatos = new ArrayList<>();

        custodyRepository.findEmCurso(animal.getAnimalId()).ifPresent(custodia -> {
            Person pessoa = custodia.getHolderPerson();
            if (pessoa != null) {
                contatos.add(new Contato(pessoa.getName(), pessoa.getPhone(), Contato.TUTOR));
                return;
            }

            // quem responde e uma organizacao: o abrigo que resgatou o animal, sem tutor humano
            Organization abrigo = custodia.getHolderOrganization();
            if (abrigo != null) {
                contatos.add(paraOrganizacao(abrigo));
            }
        });

        Map<UUID, Organization> organizacoes = new LinkedHashMap<>();

        grantRepository
                .findVigentesDeOrganizacoesNoAnimal(animal.getAnimalId(), LocalDateTime.now())
                .stream()
                .map(Grant::getGranteeOrganization)
                .forEach(organizacao -> organizacoes.put(organizacao.getOrganizationId(), organizacao));

        organizacoes.values().stream()
                .filter(organizacao -> contatos.stream()
                        .noneMatch(ja -> organizacao.getName().equals(ja.name())))
                .map(this::paraOrganizacao)
                .forEach(contatos::add);

        return contatos;
    }

    private Contato paraOrganizacao(Organization organizacao) {
        return new Contato(organizacao.getName(), organizacao.getPhone(), Contato.ORGANIZACAO);
    }

}
