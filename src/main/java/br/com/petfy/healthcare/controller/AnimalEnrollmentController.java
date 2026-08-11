package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.EnrollmentResponseDTO;
import br.com.petfy.healthcare.service.CrecheService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * As matriculas do animal, pelo lado de quem alcanca o ANIMAL — e nao de quem e da creche.
 *
 * <b>Controller proprio, e nao um metodo no AnimalController.</b> A primeira versao morava la, e o
 * {@code CorsConfigTest} — um slice que monta so alguns beans — quebrou os sete casos dele com
 * "no qualifying bean of type CrecheService". O teste estava certo sobre algo que eu nao tinha
 * pensado: pendurar a creche no controller de animal faz todo mundo que monta o AnimalController
 * carregar a creche junto.
 *
 * <b>O caminho e do tutor de proposito.</b> Quem le aqui e quem alcanca o animal: o tutor precisa
 * ver o que a creche esta esperando dele — "falta a antirrabica em dia" — sem ser membro de creche
 * nenhuma. A comprovacao que volta e a mesma que a creche ve, porque e a mesma verdade.
 */
@RestController
@RequestMapping("/animals/{animalId}/enrollments")
@RequiredArgsConstructor
public class AnimalEnrollmentController {

    private final CrecheService crecheService;

    @GetMapping
    @Operation(summary = "As matriculas do animal, com a comprovacao de saude de cada uma")
    public ResponseEntity<List<EnrollmentResponseDTO>> listEnrollments(@PathVariable UUID animalId) {
        return ResponseEntity.ok(crecheService.listEnrollmentsOfAnimal(animalId));
    }
}
