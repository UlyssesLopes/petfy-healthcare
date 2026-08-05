package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.AttachmentResponseDTO;
import br.com.petfy.healthcare.domain.entity.Attachment;
import br.com.petfy.healthcare.domain.entity.AttachmentKind;
import br.com.petfy.healthcare.domain.entity.HealthRecord;
import br.com.petfy.healthcare.domain.entity.Pet;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import br.com.petfy.healthcare.domain.repository.AttachmentRepository;
import br.com.petfy.healthcare.domain.repository.HealthRecordRepository;
import br.com.petfy.healthcare.domain.repository.VaccineRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentOwnerProvider;
import br.com.petfy.healthcare.security.PetAccessGuard;
import br.com.petfy.healthcare.service.AttachmentContent;
import br.com.petfy.healthcare.service.AttachmentService;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import br.com.petfy.healthcare.storage.AttachmentStorage;
import br.com.petfy.healthcare.storage.StoredFile;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final VaccineRepository vaccineRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final AttachmentStorage attachmentStorage;
    private final PetAccessGuard petAccessGuard;
    private final CurrentOwnerProvider currentOwnerProvider;

    @Value("${petfy.attachments.max-size-bytes:10485760}")
    private long maxSizeBytes;

    /**
     * Anexar e escrita: o arquivo passa a fazer parte do prontuario do pet, e quem so
     * acompanha nao acrescenta nada a ele.
     *
     * A ordem e storage primeiro, banco depois. Invertida, o registro existiria
     * apontando para um arquivo que a gravacao pode nao ter conseguido escrever, e o
     * download responderia 500 num anexo que a listagem jura existir. Assim o pior caso
     * e arquivo no disco sem linha no banco - invisivel, desperdicio de espaco, e nunca
     * um anexo quebrado na cara do tutor.
     */
    @Override
    @Transactional
    public AttachmentResponseDTO upload(UUID petId, MultipartFile file,
                                        UUID vaccineId, UUID healthRecordId, String description) {
        Pet pet = petAccessGuard.requireEscrita(petId);

        recusarSeVazio(file);
        recusarSeGrandeDemais(file.getSize());

        AttachmentKind kind = detectarTipo(file);

        Vaccine vaccine = vaccineId != null ? buscarVacinaDoPet(vaccineId, petId) : null;
        HealthRecord healthRecord = healthRecordId != null
                ? buscarAtendimentoDoPet(healthRecordId, petId)
                : null;

        StoredFile armazenado;
        try (InputStream content = file.getInputStream()) {
            armazenado = attachmentStorage.store(petId, content);
        } catch (IOException e) {
            throw falhaDeLeitura();
        }

        // o tamanho vem do que foi escrito, e nao do Content-Length que o cliente
        // declarou: e o unico numero que corresponde ao arquivo que existe
        recusarSeGrandeDemais(armazenado.sizeBytes());

        Attachment anexo = attachmentRepository.save(Attachment.builder()
                .pet(pet)
                .vaccine(vaccine)
                .healthRecord(healthRecord)
                .originalFilename(nomeSeguro(file.getOriginalFilename()))
                .contentType(kind.getContentType())
                .sizeBytes(armazenado.sizeBytes())
                .checksumSha256(armazenado.checksumSha256())
                .storageKey(armazenado.storageKey())
                .description(description)
                .uploadedBy(currentOwnerProvider.require())
                .creationDate(LocalDateTime.now())
                .build());

        return toResponse(anexo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttachmentResponseDTO> listByPet(UUID petId, UUID vaccineId, UUID healthRecordId) {
        petAccessGuard.requireLeitura(petId);

        List<Attachment> anexos;

        if (vaccineId != null) {
            anexos = attachmentRepository.findByVaccineVaccineIdOrderByCreationDateDesc(vaccineId);
        } else if (healthRecordId != null) {
            anexos = attachmentRepository.findByHealthRecordHealthRecordIdOrderByCreationDateDesc(healthRecordId);
        } else {
            anexos = attachmentRepository.findByPetPetIdOrderByCreationDateDesc(petId);
        }

        // o filtro por pet vale mesmo quando a consulta foi por vacina ou atendimento:
        // sem ele, passar o id de uma vacina de outro pet devolveria os anexos dela, e
        // a autorizacao teria sido feita sobre o pet errado
        return anexos.stream()
                .filter(a -> a.getPet().getPetId().equals(petId))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Autoriza pelo pet do anexo, e nao por um petId que o cliente informe.
     *
     * A rota de download nao tem petId justamente por isso: pedi-lo abriria a
     * possibilidade de autorizar contra um pet e servir o arquivo de outro.
     */
    @Override
    @Transactional(readOnly = true)
    public AttachmentContent download(UUID attachmentId) {
        Attachment anexo = attachmentRepository.findById(attachmentId)
                .orElseThrow(AttachmentServiceImpl::naoEncontrado);

        petAccessGuard.requireLeitura(anexo.getPet().getPetId());

        return new AttachmentContent(
                anexo.getOriginalFilename(),
                anexo.getContentType(),
                anexo.getSizeBytes(),
                attachmentStorage.read(anexo.getStorageKey()));
    }

    /**
     * Apaga a linha e o arquivo, nessa ordem.
     *
     * O arquivo sai depois da linha porque a transacao governa o banco, nao o disco: se
     * o delete da linha falhar, o arquivo continua la e o anexo segue inteiro. Ao
     * contrario, o tutor veria um anexo que o download nao consegue abrir.
     */
    @Override
    @Transactional
    public void delete(UUID attachmentId) {
        Attachment anexo = attachmentRepository.findById(attachmentId)
                .orElseThrow(AttachmentServiceImpl::naoEncontrado);

        petAccessGuard.requireEscrita(anexo.getPet().getPetId());

        String storageKey = anexo.getStorageKey();

        attachmentRepository.delete(anexo);
        attachmentRepository.flush();

        attachmentStorage.delete(List.of(storageKey));
    }

    /**
     * Le os primeiros bytes e decide pelo conteudo.
     *
     * O {@code Content-Type} do upload nao e consultado em momento nenhum: ele vem do
     * cliente, e um executavel renomeado para {@code .pdf} chega anunciado como PDF.
     * O que fica gravado tambem e o tipo detectado - se o banco guardasse o declarado, o
     * download devolveria o cabecalho que o atacante escolheu.
     *
     * {@code mark/reset} sobre BufferedInputStream para nao consumir o stream: os bytes
     * lidos aqui precisam continuar disponiveis para a gravacao.
     */
    private AttachmentKind detectarTipo(MultipartFile file) {
        try (InputStream stream = new BufferedInputStream(file.getInputStream())) {
            byte[] inicio = stream.readNBytes(AttachmentKind.bytesNecessarios());

            return AttachmentKind.detectar(inicio)
                    .orElseThrow(() -> new PetfyHealthcareException(
                            ErrorMessageEnum.ATTACHMENT_TYPE_NOT_ALLOWED.getMessage(),
                            ErrorMessageEnum.ATTACHMENT_TYPE_NOT_ALLOWED.getCode(),
                            HttpStatus.UNSUPPORTED_MEDIA_TYPE));

        } catch (IOException e) {
            throw falhaDeLeitura();
        }
    }

    /**
     * Vacina tem de ser do mesmo pet.
     *
     * Sem esta checagem, informar o id de uma vacina de outro pet penduraria o anexo
     * nela - o CHECK do banco nao pega isso, porque a coluna esta preenchida com um id
     * valido. A resposta e 404 e nao 403: quem pergunta nao pode descobrir que aquela
     * vacina existe.
     */
    private Vaccine buscarVacinaDoPet(UUID vaccineId, UUID petId) {
        return vaccineRepository.findById(vaccineId)
                .filter(v -> v.getPet().getPetId().equals(petId))
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.VACCINE_NOT_FOUND.getMessage(),
                        ErrorMessageEnum.VACCINE_NOT_FOUND.getCode(),
                        HttpStatus.NOT_FOUND));
    }

    private HealthRecord buscarAtendimentoDoPet(UUID healthRecordId, UUID petId) {
        return healthRecordRepository.findById(healthRecordId)
                .filter(r -> r.getPet().getPetId().equals(petId))
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.HEALTH_RECORD_NOT_FOUND.getMessage(),
                        ErrorMessageEnum.HEALTH_RECORD_NOT_FOUND.getCode(),
                        HttpStatus.NOT_FOUND));
    }

    private void recusarSeVazio(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.ATTACHMENT_EMPTY.getMessage(),
                    ErrorMessageEnum.ATTACHMENT_EMPTY.getCode(),
                    HttpStatus.BAD_REQUEST);
        }
    }

    private void recusarSeGrandeDemais(long tamanho) {
        if (tamanho > maxSizeBytes) {
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.ATTACHMENT_TOO_LARGE.getMessage(),
                    ErrorMessageEnum.ATTACHMENT_TOO_LARGE.getCode(),
                    HttpStatus.PAYLOAD_TOO_LARGE);
        }
    }

    /**
     * Guarda o nome so para exibir, sem separador de caminho e sem passar do limite da
     * coluna. Ele nunca monta caminho - a chave de storage e gerada -, mas um nome com
     * {@code ../} aparecendo na tela ou num header de download nao ajuda ninguem.
     */
    private String nomeSeguro(String original) {
        if (original == null || original.isBlank()) {
            return "anexo";
        }

        String limpo = original.replaceAll("[/\\\\]", "_").trim();

        return limpo.length() <= 255 ? limpo : limpo.substring(limpo.length() - 255);
    }

    private static PetfyHealthcareException naoEncontrado() {
        return new PetfyHealthcareException(
                ErrorMessageEnum.ATTACHMENT_NOT_FOUND.getMessage(),
                ErrorMessageEnum.ATTACHMENT_NOT_FOUND.getCode(),
                HttpStatus.NOT_FOUND);
    }

    private PetfyHealthcareException falhaDeLeitura() {
        return new PetfyHealthcareException(
                ErrorMessageEnum.ATTACHMENT_STORAGE_FAILURE.getMessage(),
                ErrorMessageEnum.ATTACHMENT_STORAGE_FAILURE.getCode(),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private AttachmentResponseDTO toResponse(Attachment anexo) {
        return AttachmentResponseDTO.builder()
                .attachmentId(anexo.getAttachmentId())
                .petId(anexo.getPet().getPetId())
                .vaccineId(anexo.getVaccine() != null ? anexo.getVaccine().getVaccineId() : null)
                .healthRecordId(anexo.getHealthRecord() != null
                        ? anexo.getHealthRecord().getHealthRecordId()
                        : null)
                .originalFilename(anexo.getOriginalFilename())
                .contentType(anexo.getContentType())
                .sizeBytes(anexo.getSizeBytes())
                .checksumSha256(anexo.getChecksumSha256())
                .description(anexo.getDescription())
                .uploadedByOwnerName(anexo.getUploadedBy() != null ? anexo.getUploadedBy().getName() : null)
                .creationDate(anexo.getCreationDate())
                .build();
    }

}
