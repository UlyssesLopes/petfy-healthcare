package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.SharedVaccineCardDTO;
import br.com.petfy.healthcare.service.PetShareService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rota publica: quem recebe o link nao tem conta no Petfy. O token no path e a
 * unica credencial, e por isso e gerado com 32 bytes de SecureRandom.
 */
@RestController
@RequestMapping("/share")
@RequiredArgsConstructor
public class SharedCardController {

    private final PetShareService petShareService;

    @GetMapping("/{token}")
    public ResponseEntity<SharedVaccineCardDTO> viewSharedCard(@PathVariable String token) {
        return ResponseEntity.ok(petShareService.viewSharedCard(token));
    }

}
