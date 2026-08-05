package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.PetHealthConditionRequestDTO;
import br.com.petfy.healthcare.domain.dto.PetHealthConditionResponseDTO;
import br.com.petfy.healthcare.domain.entity.Pet;
import br.com.petfy.healthcare.domain.entity.PetHealthCondition;
import br.com.petfy.healthcare.domain.entity.PetHealthConditionKind;
import br.com.petfy.healthcare.domain.entity.PetHealthConditionSeverity;
import br.com.petfy.healthcare.domain.repository.PetHealthConditionRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.PetAccessGuard;
import br.com.petfy.healthcare.service.PetHealthConditionService;
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
public class PetHealthConditionServiceImpl implements PetHealthConditionService {

    private final PetHealthConditionRepository petHealthConditionRepository;
    private final PetAccessGuard petAccessGuard;

    /**
     * Registrar condicao e escrita: entra no prontuario e muda a leitura de todo o resto
     * dele. Quem so acompanha nao acrescenta.
     */
    @Override
    @Transactional
    public PetHealthConditionResponseDTO create(UUID petId, PetHealthConditionRequestDTO request) {
        Pet pet = petAccessGuard.requireEscrita(petId);

        recusarGravidadeForaDeAlergia(request.getKind(), request.getSeverity());

        return toResponse(petHealthConditionRepository.save(PetHealthCondition.builder()
                .pet(pet)
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
    public List<PetHealthConditionResponseDTO> listByPet(UUID petId) {
        petAccessGuard.requireLeitura(petId);

        return petHealthConditionRepository.findByPetOrdenadasPorRelevancia(petId)
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
    public PetHealthConditionResponseDTO update(UUID petId, UUID conditionId,
                                                PetHealthConditionRequestDTO request) {
        petAccessGuard.requireEscrita(petId);

        PetHealthCondition condicao = buscarDoPet(conditionId, petId);

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

        return toResponse(petHealthConditionRepository.save(condicao));
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
    public void delete(UUID petId, UUID conditionId) {
        petAccessGuard.requireEscrita(petId);

        petHealthConditionRepository.delete(buscarDoPet(conditionId, petId));
    }

    /**
     * A regra tambem existe no banco, por CHECK, e as duas nao sao redundantes: o banco
     * impede que um insert direto contorne, e o servico impede que o cliente receba erro
     * de integridade como se fosse falha nossa. Aqui a resposta e 400, e nao 500.
     */
    private void recusarGravidadeForaDeAlergia(PetHealthConditionKind kind,
                                               PetHealthConditionSeverity severity) {
        if (severity != null && kind != PetHealthConditionKind.ALERGIA) {
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.SEVERITY_ONLY_FOR_ALLERGY.getMessage(),
                    ErrorMessageEnum.SEVERITY_ONLY_FOR_ALLERGY.getCode(),
                    HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * A condicao tem de ser do pet informado. Sem o filtro, o titular de um pet leria e
     * alteraria a condicao de outro so por ter o id - e a autorizacao teria sido feita
     * sobre o pet errado. 404, e nao 403: quem pergunta nao descobre que aquele id existe.
     */
    private PetHealthCondition buscarDoPet(UUID conditionId, UUID petId) {
        return petHealthConditionRepository.findById(conditionId)
                .filter(c -> c.getPet().getPetId().equals(petId))
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.CONDITION_NOT_FOUND.getMessage(),
                        ErrorMessageEnum.CONDITION_NOT_FOUND.getCode(),
                        HttpStatus.NOT_FOUND));
    }

    private PetHealthConditionResponseDTO toResponse(PetHealthCondition c) {
        return PetHealthConditionResponseDTO.builder()
                .petHealthConditionId(c.getPetHealthConditionId())
                .petId(c.getPet().getPetId())
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
