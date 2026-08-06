package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.PetTutorResponseDTO;
import br.com.petfy.healthcare.service.PetTutorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Aceitar o convite de co-tutor.
 *
 * Fora de {@code /animals/{animalId}} de proposito: quem aceita ainda nao alcanca o
 * animal, e so descobre de que animal se trata depois de o convite ser validado. Pedir
 * o animalId na URL exigiria do cliente um dado que ele nao tem - e daria de graca
 * um jeito de testar se um animalId existe.
 *
 * Continua exigindo autenticacao, ao contrario do {@code /share/{token}}: o
 * convite cria vinculo, e vinculo precisa de conta a que associar.
 */
@RestController
@RequestMapping("/pet-tutor-invites")
@RequiredArgsConstructor
public class PetTutorInviteController {

    private final PetTutorService petTutorService;

    @PostMapping("/{token}/accept")
    public ResponseEntity<PetTutorResponseDTO> accept(@PathVariable String token) {
        return ResponseEntity.status(HttpStatus.CREATED).body(petTutorService.accept(token));
    }

}
