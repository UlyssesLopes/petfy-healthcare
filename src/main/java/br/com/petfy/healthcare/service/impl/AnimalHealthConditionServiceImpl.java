package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.AnimalHealthConditionRequestDTO;
import br.com.petfy.healthcare.domain.dto.AnimalHealthConditionResponseDTO;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.AnimalHealthCondition;
import br.com.petfy.healthcare.domain.entity.AnimalHealthConditionKind;
import br.com.petfy.healthcare.domain.entity.AnimalHealthConditionSeverity;
import br.com.petfy.healthcare.domain.repository.AnimalHealthConditionRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.AnimalAccessGuard;
import br.com.petfy.healthcare.service.AnimalHealthConditionService;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnimalHealthConditionServiceImpl implements AnimalHealthConditionService {

    private final AnimalHealthConditionRepository animalHealthConditionRepository;
    private final AnimalAccessGuard animalAccessGuard;

    /**
     * Registrar condicao e escrita: entra no prontuario e muda a leitura de todo o resto
     * dele. Quem so acompanha nao acrescenta.
     */
    @Override
    @Transactional
    public AnimalHealthConditionResponseDTO create(UUID animalId, AnimalHealthConditionRequestDTO request) {
        Animal animal = animalAccessGuard.requireEscrita(animalId);

        recusarGravidadeForaDeAlergia(request.getKind(), request.getSeverity());

        return toResponse(animalHealthConditionRepository.save(AnimalHealthCondition.builder()
                .animal(animal)
                .kind(request.getKind())
                .description(request.getDescription())
                .severity(request.getSeverity())
                .notes(request.getNotes())
                .since(request.getSince())
                .resolvedAt(request.getResolvedAt())
                .creationDate(LocalDateTime.now())
                .build()));
    }

    /** Ler exige apenas leitura: o VIEWER precisa saber a que o animal e alergico. */
    @Override
    @Transactional(readOnly = true)
    public List<AnimalHealthConditionResponseDTO> listByAnimal(UUID animalId) {
        animalAccessGuard.requireLeitura(animalId);

        return animalHealthConditionRepository.findByAnimalOrdenadasPorRelevancia(animalId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Atualizacao parcial, como nos outros PUT do projeto: campo ausente preserva o valor.
     *
     * <b>{@code kind} nao muda.</b> Alergia que virasse condicao cronica deixaria a
     * gravidade pendurada num tipo que nao a aceita, e o banco recusaria - mas a resposta
     * certa nao e 500: e recriar o registro, porque trocar o tipo nao e corrigir um campo,
     * e dizer que era outra coisa desde o comeco.
     */
    @Override
    @Transactional
    public AnimalHealthConditionResponseDTO update(UUID animalId, UUID conditionId,
                                                AnimalHealthConditionRequestDTO request) {
        animalAccessGuard.requireEscrita(animalId);

        AnimalHealthCondition condicao = buscarDoAnimal(conditionId, animalId);

        if (request.getKind() != null && request.getKind() != condicao.getKind()) {
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.CONDITION_KIND_IS_IMMUTABLE.getMessage(),
                    ErrorMessageEnum.CONDITION_KIND_IS_IMMUTABLE.getCode(),
                    HttpStatus.CONFLICT);
        }

        if (request.getDescription() != null) condicao.setDescription(request.getDescription());
        if (request.getNotes() != null) condicao.setNotes(request.getNotes());
        if (request.getSince() != null) condicao.setSince(request.getSince());
        if (request.getResolvedAt() != null) condicao.setResolvedAt(request.getResolvedAt());

        if (request.getSeverity() != null) {
            // contra o tipo JA GRAVADO, e nao contra o do request: o kind e imutavel, e
            // validar contra o que veio no payload deixaria passar gravidade em condicao
            // cronica sempre que o cliente omitisse o kind
            recusarGravidadeForaDeAlergia(condicao.getKind(), request.getSeverity());
            condicao.setSeverity(request.getSeverity());
        }

        condicao.setUpdateDate(LocalDateTime.now());

        return toResponse(animalHealthConditionRepository.save(condicao));
    }

    /**
     * Apagar de verdade existe para o registro criado por engano.
     *
     * Nao e o caminho de "a condicao passou" - esse e preencher {@code resolvedAt}, que
     * encerra sem esconder que existiu. Os dois precisam existir: apagar o que foi
     * digitado errado nao e o mesmo que reescrever o historico do animal.
     */
    @Override
    @Transactional
    public void delete(UUID animalId, UUID conditionId) {
        animalAccessGuard.requireEscrita(animalId);

        animalHealthConditionRepository.delete(buscarDoAnimal(conditionId, animalId));
    }

    /**
     * A regra tambem existe no banco, por CHECK, e as duas nao sao redundantes: o banco
     * impede que um insert direto contorne, e o servico impede que o cliente receba erro
     * de integridade como se fosse falha nossa. Aqui a resposta e 400, e nao 500.
     */
    private void recusarGravidadeForaDeAlergia(AnimalHealthConditionKind kind,
                                               AnimalHealthConditionSeverity severity) {
        if (severity != null && kind != AnimalHealthConditionKind.ALERGIA) {
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.SEVERITY_ONLY_FOR_ALLERGY.getMessage(),
                    ErrorMessageEnum.SEVERITY_ONLY_FOR_ALLERGY.getCode(),
                    HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * A condicao tem de ser do animal informado. Sem o filtro, o titular de um animal leria e
     * alteraria a condicao de outro so por ter o id - e a autorizacao teria sido feita
     * sobre o animal errado. 404, e nao 403: quem pergunta nao descobre que aquele id existe.
     */
    private AnimalHealthCondition buscarDoAnimal(UUID conditionId, UUID animalId) {
        return animalHealthConditionRepository.findById(conditionId)
                .filter(c -> c.getAnimal().getAnimalId().equals(animalId))
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.CONDITION_NOT_FOUND.getMessage(),
                        ErrorMessageEnum.CONDITION_NOT_FOUND.getCode(),
                        HttpStatus.NOT_FOUND));
    }

    private AnimalHealthConditionResponseDTO toResponse(AnimalHealthCondition c) {
        return AnimalHealthConditionResponseDTO.builder()
                .animalHealthConditionId(c.getAnimalHealthConditionId())
                .animalId(c.getAnimal().getAnimalId())
                .kind(c.getKind())
                .description(c.getDescription())
                .severity(c.getSeverity())
                .notes(c.getNotes())
                .since(c.getSince())
                .resolvedAt(c.getResolvedAt())
                .ativa(c.isAtiva())
                .creationDate(c.getCreationDate())
                .updateDate(c.getUpdateDate())
                .build();
    }

}
