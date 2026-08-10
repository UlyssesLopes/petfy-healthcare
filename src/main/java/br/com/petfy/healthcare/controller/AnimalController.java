package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.AnimalRequestDTO;
import br.com.petfy.healthcare.domain.dto.AnimalResponseDTO;
import br.com.petfy.healthcare.service.AnimalService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/animals")
@RequiredArgsConstructor
public class AnimalController {

    private final AnimalService animalService;

    /*
     * A rota de matriculas do animal saiu daqui e virou o `AnimalEnrollmentController`. Nao foi
     * organizacao: o `CorsConfigTest` — um slice que monta so alguns beans — quebrou os sete casos
     * dele quando este controller passou a depender do CrecheService. Pendurar a creche no
     * controller de animal faz todo mundo que monta o AnimalController carregar a creche junto.
     */

    @Operation(summary = "Cadastra um animal", description = "Quem cadastra passa a deter a custodia dele. Especie e obrigatoria porque o catalogo de vacina depende dela; identificacao - microchip, tatuagem, RGA - e toda opcional, porque metade dos animais do Brasil nao tem nenhuma.")
    @PostMapping
    public ResponseEntity<AnimalResponseDTO> createAnimal(@Valid @RequestBody AnimalRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(animalService.createAnimal(dto));
    }

    @Operation(summary = "Um animal", description = "Animal fora do seu alcance responde 404 e nao 403: dizer que existe e voce nao pode ver ja e informacao sobre ele.")
    @GetMapping("/{animalId}")
    public ResponseEntity<AnimalResponseDTO> getAnimal(@PathVariable UUID animalId) {
        return ResponseEntity.ok(animalService.getAnimalById(animalId));
    }

    @Operation(summary = "Os animais que eu alcanco",
               description = "Por custodia ou por concessao vigente. Paginada, com teto de 100 por "
                             + "pagina. A leitura em largura da area de organizacao, que le centenas, "
                             + "e outra: /professional/animals, com busca e ordem.")
    @GetMapping
    public ResponseEntity<Page<AnimalResponseDTO>> listAll(
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(animalService.listAllAnimals(pageable));
    }

    /**
     * Atualizacao parcial: so os campos enviados sao alterados. Campos ausentes ou
     * nulos no payload sao ignorados e o valor existente e preservado. Por isso
     * @Valid nao e aplicado aqui - nenhum campo e obrigatorio numa atualizacao
     * parcial.
     */
    @Operation(summary = "Atualiza dados do animal",
               description = "Atualizacao parcial: apenas os campos presentes no payload sao alterados. " +
                             "Campos ausentes ou nulos preservam o valor existente. " +
                             "Nenhum campo e obrigatorio neste endpoint.")
    @PutMapping("/{animalId}")
    public ResponseEntity<AnimalResponseDTO> updateAnimal(@PathVariable UUID animalId, @RequestBody AnimalRequestDTO dto) {
        return ResponseEntity.ok(animalService.updateAnimal(animalId, dto));
    }

    @Operation(summary = "Apaga o animal e todo o rastro dele", description = "Irreversivel, e apaga a carteira, o prontuario, o peso, os anexos e os arquivos no disco. Se houver outro tutor, o animal sobrevive e a titularidade passa a ele.")
    @DeleteMapping("/{animalId}")
    public ResponseEntity<Void> deleteAnimal(@PathVariable UUID animalId) {
        animalService.deleteAnimal(animalId);
        return ResponseEntity.noContent().build();
    }

}
