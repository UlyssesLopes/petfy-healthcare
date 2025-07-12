package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.PetResponseDTO;
import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.entity.Pet;
import br.com.petfy.healthcare.domain.repository.OwnerRepository;
import br.com.petfy.healthcare.domain.repository.PetRepository;
import br.com.petfy.healthcare.service.PetIdService;
import lombok.RequiredArgsConstructor;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


@Service
@RequiredArgsConstructor
public class PetIdServiceImpl implements PetIdService {

    private final TesseractOcrServiceImpl tesseractOcrService;
    private final ImageProcessorService imageProcessorService;
    private final PetRepository petRepository;
    private final OwnerRepository ownerRepository;

    private static final List<String> knownFields = List.of(
            "Nome do Animal", "Registro Geral do Animal", "Microchip",
            "Castrado", "Cor", "Espécie", "Sexo",
            "Data de Nascimento", "Naturalidade"
    );

    @Override
    public PetResponseDTO importPetFromIdCard(MultipartFile file) throws IOException, TesseractException {

        BufferedImage imageFile = ImageIO.read(file.getInputStream());
        BufferedImage bufferedImage = imageProcessorService.preProcess(imageFile);

        String extractedText = tesseractOcrService.extractText(bufferedImage);

        Map<String, String> stringStringMap = parseFields(extractedText);

        PetResponseDTO petResponseDTO = parse(stringStringMap);

        Owner ownerById = ownerRepository.findById(UUID.fromString("386b64d9-eebe-4122-b4cf-5b128316b79f")).get();

        Pet pet = Pet.builder()
                .name(petResponseDTO.getName())
                .owner(ownerById)
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
        petResponse.setMicrochip(Boolean.valueOf(stringStringMap.get("Microship")));
        petResponse.setColor(stringStringMap.get("Cor"));
        petResponse.setBreed(stringStringMap.get("Espécie"));
        petResponse.setGender(stringStringMap.get("Sexo"));
        petResponse.setBornDate(bornDateStringFormat(stringStringMap.get("Data de Nascimento")));
        petResponse.setBornLocal(stringStringMap.get("Naturalidade"));

        return petResponse;
    }

    public LocalDate bornDateStringFormat(String bornStringDate) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return LocalDate.parse(bornStringDate, dateTimeFormatter);
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
