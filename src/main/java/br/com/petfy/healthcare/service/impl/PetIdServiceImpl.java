package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.AnimalResponseDTO;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.PetTutor;
import br.com.petfy.healthcare.domain.entity.PetTutorRole;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.repository.PersonRepository;
import br.com.petfy.healthcare.domain.repository.AnimalRepository;
import br.com.petfy.healthcare.domain.repository.PetTutorRepository;
import br.com.petfy.healthcare.security.CurrentPersonProvider;
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
    private final AnimalRepository animalRepository;
    private final PetTutorRepository petTutorRepository;
    private final CurrentPersonProvider currentPersonProvider;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final List<String> knownFields = List.of(
            "Nome do Animal", "Registro Geral do Animal", "Microchip",
            "Castrado", "Cor", "Espécie", "Raça", "Sexo",
            "Data de Nascimento", "Naturalidade"
    );

    @Override
    public AnimalResponseDTO importAnimalFromIdCard(MultipartFile file) throws IOException, TesseractException {

        Person personById = currentPersonProvider.require();

        BufferedImage imageFile = ImageIO.read(file.getInputStream());
        BufferedImage bufferedImage = imageProcessorService.preProcess(imageFile);

        String extractedText = tesseractOcrService.extractText(bufferedImage);

        Map<String, String> stringStringMap = parseFields(extractedText);

        AnimalResponseDTO animalResponseDTO = parse(stringStringMap);

        Animal animal = Animal.builder()
                .name(animalResponseDTO.getName())
                .type(animalResponseDTO.getType())
                .breed(animalResponseDTO.getBreed())
                .bornDate(animalResponseDTO.getBornDate())
                .gender(animalResponseDTO.getGender())
                .bornLocal(animalResponseDTO.getBornLocal())
                .color(animalResponseDTO.getColor())
                .generalRegistry(animalResponseDTO.getGeneralRegistry())
                .creationDate(LocalDateTime.now())
                .microchip(animalResponseDTO.getMicrochip())
                .build();

        Animal saved = animalRepository.save(animal);

        // quem importou a carteirinha nasce titular do animal, igual a quem cadastra
        // pela API - a partir da V15 o vinculo e explicito, nao um campo no animal
        petTutorRepository.save(PetTutor.builder()
                .animal(saved)
                .person(personById)
                .role(PetTutorRole.HOLDER)
                .creationDate(LocalDateTime.now())
                .build());

        return toResponse(saved);
    }

    public AnimalResponseDTO parse(Map<String, String> stringStringMap) {
        AnimalResponseDTO animalResponse = new AnimalResponseDTO();
        animalResponse.setName(stringStringMap.get("Nome do Animal"));
        animalResponse.setGeneralRegistry(stringStringMap.get("Registro Geral do Animal"));
        animalResponse.setMicrochip(parseSimNao(stringStringMap.get("Microchip")));
        animalResponse.setColor(stringStringMap.get("Cor"));
        // "Especie" e canina/felina, que corresponde ao type; a raca vem em campo proprio
        animalResponse.setType(stringStringMap.get("Espécie"));
        animalResponse.setBreed(stringStringMap.get("Raça"));
        animalResponse.setGender(stringStringMap.get("Sexo"));
        animalResponse.setBornDate(bornDateStringFormat(stringStringMap.get("Data de Nascimento")));
        animalResponse.setBornLocal(stringStringMap.get("Naturalidade"));

        return animalResponse;
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

    private AnimalResponseDTO toResponse(Animal animal) {
        return AnimalResponseDTO.builder()
                .animalId(animal.getAnimalId())
                .name(animal.getName())
                .type(animal.getType())
                .generalRegistry(animal.getGeneralRegistry())
                .breed(animal.getBreed())
                .bornDate(animal.getBornDate())
                .weight(animal.getWeight())
                .gender(animal.getGender())
                .personId(animal.getHolder().map(Person::getPersonId).orElse(null))
                .creationDate(animal.getCreationDate())
                .updateDate(animal.getUpdateDate())
                .build();
    }

}
