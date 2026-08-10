package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.HealthRecordCorrectionResponseDTO;
import br.com.petfy.healthcare.domain.dto.HealthRecordRequestDTO;
import br.com.petfy.healthcare.domain.dto.HealthRecordResponseDTO;
import br.com.petfy.healthcare.domain.dto.VaccineCorrectionResponseDTO;
import br.com.petfy.healthcare.domain.dto.VaccineRequestDTO;
import br.com.petfy.healthcare.domain.dto.VaccineResponseDTO;
import br.com.petfy.healthcare.domain.dto.VetPetDTO;
import br.com.petfy.healthcare.service.VetPetService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Prefixo /professional separado do /animals do tutor de proposito: sao visoes
 * diferentes do mesmo recurso, com regras de acesso diferentes. Misturar as duas no
 * mesmo path faria a autorizacao depender de quem chamou, que e onde esse tipo de
 * bug mora. (O prefixo era /vet ate a Fase 6 dissolver o papel.)
 *
 * Nao ha DELETE em nenhum dos dois recursos: apagar registro de saude nao e
 * correcao - ver README.
 */
@RestController
@RequestMapping("/professional/animals")
@RequiredArgsConstructor
public class ProfessionalAnimalController {

    private final VetPetService vetPetService;

    /**
     * A leitura em largura da area de organizacao (PRODUTO 9.3): lista, busca e
     * ordem. O teto de 100 por pagina nao esta aqui porque e global, em
     * {@code spring.data.web.pageable.max-page-size}.
     *
     * Ordem default por nome do animal, e nao por quando o acesso foi concedido: o
     * expediente procura um animal pelo nome que o tutor falou no balcao.
     */
    @GetMapping
    @Operation(summary = "Os animais que o contexto ativo alcanca, com busca e ordem")
    public ResponseEntity<Page<VetPetDTO>> listAccessibleAnimals(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "animal.name") Pageable pageable) {
        return ResponseEntity.ok(vetPetService.listAccessibleAnimals(q, pageable));
    }

    /**
     * Os animais sob custodia da organizacao — o abrigo, e nao a clinica.
     *
     * <b>Rota separada de proposito, e o segmento literal vem antes do {@code {animalId}}</b>
     * na resolucao do Spring, entao "in-custody" nunca e lido como id. As duas listas respondem
     * perguntas diferentes: a de cima e "quem eu alcanco porque alguem me deu acesso"; esta e
     * "por quem eu respondo". Juntar as duas faria o abrigo perder de vista exatamente o que ele
     * precisa para decidir uma adocao.
     */
    @GetMapping("/in-custody")
    @Operation(summary = "Os animais sob custodia da organizacao ativa")
    public ResponseEntity<Page<VetPetDTO>> listAnimalsInCustody(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "animal.name") Pageable pageable) {
        return ResponseEntity.ok(vetPetService.listAnimalsInCustody(q, pageable));
    }

    @Operation(summary = "As vacinas do animal, pelo lado profissional")
    @GetMapping("/{animalId}/vaccines")
    public ResponseEntity<List<VaccineResponseDTO>> listVaccines(@PathVariable UUID animalId) {
        return ResponseEntity.ok(vetPetService.listVaccines(animalId));
    }

    /**
     * Sem @Valid de proposito: no VaccineRequestDTO o unico campo obrigatorio e o
     * animalId, que aqui vem do path. Exigi-lo tambem no corpo so criaria uma
     * duplicidade que o service ignora - ele usa o do path.
     */
    @Operation(summary = "Registra uma vacina no animal atendido", description = "A organizacao vem do contexto ativo e NUNCA do payload: aceitar organizationId do cliente deixaria um veterinario registrar em nome de outra clinica.")
    @PostMapping("/{animalId}/vaccines")
    public ResponseEntity<VaccineResponseDTO> registerVaccine(@PathVariable UUID animalId,
                                                              @RequestBody VaccineRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vetPetService.registerVaccine(animalId, request));
    }

    @Operation(summary = "Corrige uma vacina que a propria organizacao registrou", description = "Correcao e nao reescrita: o estado anterior fica gravado. Vale so para registro do proprio contexto e so dentro de uma janela curta - corrigir lancamento de meses atras nao e conserto de digitacao, e o tutor e quem decide o que fica na carteira dele.")
    @PutMapping("/{animalId}/vaccines/{vaccineId}")
    public ResponseEntity<VaccineResponseDTO> correctVaccine(@PathVariable UUID animalId,
                                                             @PathVariable UUID vaccineId,
                                                             @RequestBody VaccineRequestDTO request) {
        return ResponseEntity.ok(vetPetService.correctVaccine(animalId, vaccineId, request));
    }

    @Operation(summary = "O historico de correcoes da vacina")
    @GetMapping("/{animalId}/vaccines/{vaccineId}/corrections")
    public ResponseEntity<List<VaccineCorrectionResponseDTO>> listCorrections(@PathVariable UUID animalId,
                                                                              @PathVariable UUID vaccineId) {
        return ResponseEntity.ok(vetPetService.listCorrections(animalId, vaccineId));
    }

    @Operation(summary = "Os atendimentos do animal, pelo lado profissional")
    @GetMapping("/{animalId}/health-records")
    public ResponseEntity<List<HealthRecordResponseDTO>> listHealthRecords(@PathVariable UUID animalId) {
        return ResponseEntity.ok(vetPetService.listHealthRecords(animalId));
    }

    @Operation(summary = "Registra um atendimento no animal", description = "Ato clinico: exige credencial profissional ativa, e a organizacao precisa ter a capacidade de registrar ato clinico. A creche nunca escreve no prontuario.")
    @PostMapping("/{animalId}/health-records")
    public ResponseEntity<HealthRecordResponseDTO> registerHealthRecord(@PathVariable UUID animalId,
                                                                        @RequestBody HealthRecordRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vetPetService.registerHealthRecord(animalId, request));
    }

    @Operation(summary = "Corrige um atendimento que a propria organizacao registrou", description = "Mesma janela e mesma regra da correcao de vacina.")
    @PutMapping("/{animalId}/health-records/{healthRecordId}")
    public ResponseEntity<HealthRecordResponseDTO> correctHealthRecord(@PathVariable UUID animalId,
                                                                       @PathVariable UUID healthRecordId,
                                                                       @RequestBody HealthRecordRequestDTO request) {
        return ResponseEntity.ok(vetPetService.correctHealthRecord(animalId, healthRecordId, request));
    }

    @Operation(summary = "O historico de correcoes do atendimento")
    @GetMapping("/{animalId}/health-records/{healthRecordId}/corrections")
    public ResponseEntity<List<HealthRecordCorrectionResponseDTO>> listHealthRecordCorrections(
            @PathVariable UUID animalId, @PathVariable UUID healthRecordId) {
        return ResponseEntity.ok(vetPetService.listHealthRecordCorrections(animalId, healthRecordId));
    }

}
