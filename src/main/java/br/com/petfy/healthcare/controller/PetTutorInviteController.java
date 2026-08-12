package br.com.petfy.healthcare.controller;

import io.swagger.v3.oas.annotations.Operation;
import br.com.petfy.healthcare.domain.dto.PetTutorInvitePreviewResponseDTO;
import br.com.petfy.healthcare.domain.dto.PetTutorResponseDTO;
import br.com.petfy.healthcare.service.PetTutorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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

    @Operation(summary = "O convite antes de aceitar",
               description = "Nome do animal, quem convidou, o que esta sendo oferecido e ate quando "
                             + "vale. NAO CONSOME o convite: abrir o link para entender o que esta em "
                             + "jogo nao pode gastar o direito de entrar. E nao traz nada de saude — "
                             + "quem ainda nao aceitou nao alcanca o animal.")
    @GetMapping("/{token}")
    public ResponseEntity<PetTutorInvitePreviewResponseDTO> preview(@PathVariable String token) {
        return ResponseEntity.ok(petTutorService.preview(token));
    }

    @Operation(summary = "Aceita o convite de co-tutor",
               description = "Quem aceita passa a alcancar o animal por concessao - nao vira titular. "
                             + "A titularidade se transfere por rota propria, e e um fato diferente: "
                             + "dividir o cuidado nao e passar a responsabilidade.")
    @PostMapping("/{token}/accept")
    public ResponseEntity<PetTutorResponseDTO> accept(@PathVariable String token) {
        return ResponseEntity.status(HttpStatus.CREATED).body(petTutorService.accept(token));
    }

    @Operation(summary = "Recusa o convite",
               description = "Consome o convite e avisa quem convidou — 'se recusar, ele e avisado e "
                             + "nada muda para o animal'. NAO pede motivo: recusar dividir o cuidado "
                             + "de um animal e uma decisao pessoal, e um campo de justificativa faria "
                             + "o produto pedir a quem disse nao que explicasse o nao.")
    @PostMapping("/{token}/reject")
    public ResponseEntity<Void> reject(@PathVariable String token) {
        petTutorService.reject(token);
        return ResponseEntity.noContent().build();
    }

}
