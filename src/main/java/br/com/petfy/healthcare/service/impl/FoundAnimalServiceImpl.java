package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.FoundAnimalCardDTO;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.AnimalHealthCondition;
import br.com.petfy.healthcare.domain.entity.AnimalHealthConditionKind;
import br.com.petfy.healthcare.domain.entity.CareInstruction;
import br.com.petfy.healthcare.domain.repository.AnimalDeathRepository;
import br.com.petfy.healthcare.domain.repository.AnimalHealthConditionRepository;
import br.com.petfy.healthcare.domain.repository.AnimalRepository;
import br.com.petfy.healthcare.domain.repository.CareInstructionRepository;
import br.com.petfy.healthcare.domain.repository.CustodyRepository;
import br.com.petfy.healthcare.domain.repository.VaccineRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.service.AnimalContacts;
import br.com.petfy.healthcare.service.FoundAnimalService;
import br.com.petfy.healthcare.service.SensitiveAccessLogger;
import br.com.petfy.healthcare.service.VaccineStatusCalculator;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * "Achei um animal na rua" (Tela 34).
 *
 * <b>Publica e sem conta, e isso e o produto e nao um descuido:</b> "quem encontrou pode ser a
 * unica pessoa com o animal nas proximas horas". Exigir cadastro para devolver um bicho ao dono e
 * o atrito que faz a pessoa desistir e ir embora.
 *
 * <b>O que ela entrega e o cartao inteiro</b> — contatos, alergias, condicoes, medicacao em curso
 * e vacinacao —, porque esconder que o animal e alergico a frango de quem vai alimenta-lo hoje a
 * noite nao protege ninguem. <b>E o que ela nunca entrega</b> continua fora: historico clinico,
 * diagnostico, endereco.
 *
 * <b>A trava contra varredura nao esta aqui, e sim no {@code RateLimitFilter}</b>, por IP e por
 * minuto. Ela e obrigatoria: sem ela, esta rota e uma listagem de nomes e telefones indexada por
 * um numero de 15 digitos.
 */
@Service
@RequiredArgsConstructor
public class FoundAnimalServiceImpl implements FoundAnimalService {

    private final AnimalRepository animalRepository;
    private final AnimalDeathRepository animalDeathRepository;
    private final AnimalHealthConditionRepository conditionRepository;
    private final CareInstructionRepository careInstructionRepository;
    private final CustodyRepository custodyRepository;
    private final AnimalContacts animalContacts;
    private final VaccineRepository vaccineRepository;
    private final VaccineStatusCalculator vaccineStatusCalculator;
    private final SensitiveAccessLogger sensitiveAccessLogger;

    @Value("${petfy.vaccine.due-soon-window-days:30}")
    private int windowDays;

    @Override
    @Transactional
    public FoundAnimalCardDTO procurar(String microchipNumber) {
        Animal animal = escolher(animalRepository.findComMicrochip(microchipNumber.trim()));

        // registrado depois de o animal existir, e nao antes: um numero que nao esta no Petfy nao
        // e acesso a animal nenhum, e logar a tentativa encheria a tabela de linhas sem animal
        sensitiveAccessLogger.microchipProcurado(animal);

        LocalDate hoje = LocalDate.now();

        return FoundAnimalCardDTO.builder()
                .animalName(animal.getName())
                .animalType(animal.getType())
                .animalBreed(animal.getBreed())
                .animalGender(animal.getGender())
                .animalBornDate(animal.getBornDate())
                .animalWeight(animal.getWeight())
                .contacts(contatos(animal))
                .allergies(descricoes(animal, AnimalHealthConditionKind.ALERGIA))
                .conditions(descricoes(animal, AnimalHealthConditionKind.CONDICAO_CRONICA))
                .ongoingCare(emCurso(animal, hoje))
                .vaccines(vacinas(animal, hoje))
                .build();
    }

    /**
     * Qual cadastro responde, quando o microchip aparece em mais de um.
     *
     * <b>O caso e comum neste produto, e a Tela 32 existe por causa dele:</b> o gato da praca
     * cadastrado pela protetora, depois pela clinica, depois pelo abrigo. Escolher "o primeiro que
     * vier" daria a quem esta com o animal na mao o cadastro que a clinica abriu as 22h com tres
     * campos preenchidos, em vez do que tem sete anos de vida registrada.
     *
     * <b>O criterio e o mesmo que a Tela 32 ja usa para dizer qual e o cadastro real:</b> quem tem
     * alguem respondendo por ele agora vem primeiro; entre iguais, o mais antigo — que e o que
     * teve tempo de acumular a vida do animal.
     */
    private Animal escolher(List<Animal> candidatos) {
        return candidatos.stream()
                .filter(animal -> !encerrado(animal))
                .max(Comparator
                        .comparing((Animal animal) -> temQuemResponda(animal))
                        .thenComparing(Animal::getCreationDate,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .orElseThrow(FoundAnimalServiceImpl::naoEncontrado);
    }

    /**
     * Animal com a linha do tempo encerrada nao aparece na busca, e responde igual a um numero que
     * nao existe.
     *
     * <b>E a decisao menos obvia desta tela, entao vale o porque.</b> Um chip lido num animal vivo
     * que bate com um cadastro encerrado significa uma de duas coisas: o cadastro esta errado, ou
     * o numero foi digitado errado. Nos dois casos, o resultado de mostrar o cartao seria uma
     * ligacao para alguem que perdeu o animal, perguntando se o bicho dele esta na rua.
     *
     * O custo esta assumido: se o tutor encerrou por engano, quem achar o animal vai ler "este
     * numero nao esta no Petfy" — e a tela manda procurar uma clinica, que e o que a pessoa faria
     * de qualquer jeito.
     */
    private boolean encerrado(Animal animal) {
        return animalDeathRepository.existsById(animal.getAnimalId());
    }

    private boolean temQuemResponda(Animal animal) {
        return custodyRepository.findEmCurso(animal.getAnimalId()).isPresent();
    }

    /**
     * Para quem ligar: quem responde pelo animal primeiro, depois as organizacoes que o alcancam.
     *
     * <b>A ordem e a do desenho</b> — "Ligar para Marcelo Dias" acima de "Ligar para a Clinica Vet
     * Norte" —, e ela nao e cosmetica: quem responde pelo animal e quem pode ir busca-lo.
     */
    private List<FoundAnimalCardDTO.FoundContactDTO> contatos(Animal animal) {
        return animalContacts.de(animal).stream()
                .map(contato -> FoundAnimalCardDTO.FoundContactDTO.builder()
                        .name(contato.name())
                        .phone(contato.phone())
                        .kind(contato.kind())
                        .build())
                .toList();
    }

    private List<String> descricoes(Animal animal, AnimalHealthConditionKind kind) {
        return conditionRepository.findByAnimalOrdenadasPorRelevancia(animal.getAnimalId()).stream()
                .filter(condicao -> condicao.getKind() == kind)
                // condicao encerrada nao entra: dizer a quem socorre que o animal tem uma doenca
                // que ele ja nao tem faria a pessoa agir sobre um fato que deixou de valer
                .filter(condicao -> condicao.getResolvedAt() == null)
                .map(AnimalHealthCondition::getDescription)
                .toList();
    }

    private List<String> emCurso(Animal animal, LocalDate hoje) {
        return careInstructionRepository
                .findVigentesNosAnimais(List.of(animal.getAnimalId()), hoje).stream()
                .map(CareInstruction::getDescription)
                .toList();
    }

    private List<FoundAnimalCardDTO.FoundVaccineDTO> vacinas(Animal animal, LocalDate hoje) {
        return vaccineRepository
                .findByAnimalAnimalIdOrderByApplicationDateDesc(animal.getAnimalId()).stream()
                .map(vacina -> FoundAnimalCardDTO.FoundVaccineDTO.builder()
                        .vaccineName(vacina.getVaccineName())
                        .nextDoseDate(vacina.getNextDoseDate())
                        .status(vaccineStatusCalculator.classify(vacina.getNextDoseDate(), hoje, windowDays))
                        .build())
                .toList();
    }

    /**
     * O vazio diz o que fazer em seguida, e por isso tem codigo proprio.
     *
     * "Nenhum resultado encontrado" deixaria a pessoa e o animal parados na calcada. O cliente
     * traduz o 161 na caixa que manda conferir os 15 digitos e procurar uma clinica ou a
     * prefeitura — "elas consultam bases que o Petfy nao alcanca".
     */
    private static PetfyHealthcareException naoEncontrado() {
        return new PetfyHealthcareException(
                ErrorMessageEnum.MICROCHIP_NOT_FOUND.getMessage(),
                ErrorMessageEnum.MICROCHIP_NOT_FOUND.getCode(),
                HttpStatus.NOT_FOUND);
    }

}
