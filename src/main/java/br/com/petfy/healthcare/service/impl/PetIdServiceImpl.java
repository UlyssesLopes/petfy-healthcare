package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.PetResponseDTO;
import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.entity.Pet;
import br.com.petfy.healthcare.domain.repository.OwnerRepository;
import br.com.petfy.healthcare.domain.repository.PetRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.service.PetIdService;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.*;


@Slf4j
@Service
@RequiredArgsConstructor
public class PetIdServiceImpl implements PetIdService {

    private final TesseractOcrServiceImpl tesseractOcrService;
    private final ImageProcessorService imageProcessorService;
    private final PetRepository petRepository;
    private final OwnerRepository ownerRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final List<String> knownFields = List.of(
            "Nome do Animal", "Registro Geral do Animal", "Microchip",
            "Castrado", "Cor", "Espécie", "Raça", "Sexo",
            "Data de Nascimento", "Naturalidade"
    );

    @Override
    public PetResponseDTO importPetFromIdCard(UUID ownerId, MultipartFile file) throws IOException, TesseractException {

        Owner ownerById = ownerRepository.findById(ownerId)
                .orElseThrow(() -> new PetfyHealthcareException(ErrorMessageEnum.OWNER_NOT_FOUND.getMessage(), ErrorMessageEnum.OWNER_NOT_FOUND.getCode(), HttpStatus.NOT_FOUND));

        BufferedImage imageFile = ImageIO.read(file.getInputStream());
        BufferedImage bufferedImage = imageProcessorService.preProcess(imageFile);

        String extractedText = tesseractOcrService.extractText(bufferedImage);

        Map<String, String> stringStringMap = parseFields(extractedText);

        PetResponseDTO petResponseDTO = parse(stringStringMap);

        Pet pet = Pet.builder()
                .name(petResponseDTO.getName())
                .owner(ownerById)
                .type(petResponseDTO.getType())
                .breed(petResponseDTO.getBreed())
                .bornDate(petResponseDTO.getBornDate())
                .gender(petResponseDTO.getGender())
                .bornLocal(petResponseDTO.getBornLocal())
                .color(petResponseDTO.getColor())
                .generalRegistry(petResponseDTO.getGeneralRegistry())
                .creationDate(LocalDateTime.now())
                .microchip(petResponseDTO.getMicrochip())
                .build();

        Pet saved = petRepository.save(pet);
        return toResponse(saved);
    }

    public PetResponseDTO parse(Map<String, String> stringStringMap) {
        PetResponseDTO petResponse = new PetResponseDTO();
        petResponse.setName(stringStringMap.get("Nome do Animal"));
        petResponse.setGeneralRegistry(stringStringMap.get("Registro Geral do Animal"));
        petResponse.setMicrochip(parseSimNao(stringStringMap.get("Microchip")));
        petResponse.setColor(stringStringMap.get("Cor"));
        // "Especie" e canina/felina, que corresponde ao type; a raca vem em campo proprio
        petResponse.setType(stringStringMap.get("Espécie"));
        petResponse.setBreed(stringStringMap.get("Raça"));
        petResponse.setGender(stringStringMap.get("Sexo"));
        petResponse.setBornDate(bornDateStringFormat(stringStringMap.get("Data de Nascimento")));
        petResponse.setBornLocal(stringStringMap.get("Naturalidade"));

        return petResponse;
    }

    public LocalDate bornDateStringFormat(String bornStringDate) {
        if (bornStringDate == null || bornStringDate.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(bornStringDate, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            log.warn("Data de nascimento nao reconhecida no OCR: '{}'", bornStringDate);
            return null;
        }
    }

    private Boolean parseSimNao(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        String normalized = rawValue.trim().toLowerCase();
        if (normalized.startsWith("s")) {
            return Boolean.TRUE;
        }
        if (normalized.startsWith("n")) {
            return Boolean.FALSE;
        }
        log.warn("Valor booleano nao reconhecido no OCR: '{}'", rawValue);
        return null;
    }

    public Map<String, String> parseFields(String ocrText) {
        Map<String, String> fieldMap = new LinkedHashMap<>();
        String[] lines = ocrText.split("\n");

        List<String> fieldsQueue = new ArrayList<>();
        boolean expectValue = false;

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;

            if (containsField(line)) {
                List<String> fields = extractFieldsFromLine(line);
                fieldsQueue.addAll(fields);
                expectValue = true;
            }
            else if (expectValue && !fieldsQueue.isEmpty()) {
                if (fieldsQueue.size() == 1) {
                    fieldMap.put(fieldsQueue.get(0), sanitizeValue(line));
                } else {
                    List<String> values = Arrays.asList(line.split("\\s+"));

                    if (fieldsQueue.size() == 2 && values.size() >= 2) {
                        fieldMap.put(fieldsQueue.get(0), sanitizeValue(values.get(0)));
                        String restante = String.join(" ", values.subList(1, values.size()));
                        fieldMap.put(fieldsQueue.get(1), sanitizeValue(restante));
                    } else {
                        for (int i = 0; i < fieldsQueue.size(); i++) {
                            String field = fieldsQueue.get(i);
                            String value = (i < values.size()) ? values.get(i) : "";
                            fieldMap.put(field, sanitizeValue(value));
                        }
                    }
                }
                fieldsQueue.clear();
                expectValue = false;
            }
        }

        return fieldMap;
    }

    private String sanitizeValue(String rawValue) {
        if (rawValue == null) return null;
        return rawValue
                .replaceAll("-+$", "")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    private boolean containsField(String line) {
        return knownFields.stream().anyMatch(line::contains);
    }

    private List<String> extractFieldsFromLine(String line) {
        List<String> fields = new ArrayList<>();
        for (String field : knownFields) {
            if (line.contains(field)) {
                fields.add(field);
            }
        }
        // os valores da linha seguinte sao casados por posicao, entao os rotulos
        // precisam sair na ordem em que aparecem na linha - nao na ordem do knownFields
        fields.sort(Comparator.comparingInt(line::indexOf));
        return fields;
    }

    private PetResponseDTO toResponse(Pet pet) {
        return PetResponseDTO.builder()
                .petId(pet.getPetId())
                .name(pet.getName())
                .type(pet.getType())
                .generalRegistry(pet.getGeneralRegistry())
                .breed(pet.getBreed())
                .bornDate(pet.getBornDate())
                .weight(pet.getWeight())
                .gender(pet.getGender())
                .ownerId(pet.getOwner().getOwnerId())
                .creationDate(pet.getCreationDate())
                .updateDate(pet.getUpdateDate())
                .build();
    }

}
