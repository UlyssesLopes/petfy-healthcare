package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.FoundAnimalCardDTO;
import br.com.petfy.healthcare.domain.dto.FoundAnimalRequestDTO;
import br.com.petfy.healthcare.service.FoundAnimalService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rota publica: quem achou um animal na rua nao tem conta no Petfy, e nao vai criar uma as 23h
 * com o bicho no colo (Tela 34).
 *
 * <b>A credencial aqui e o proprio numero do microchip</b>, e e por isso que a busca parcial nao
 * existe e o limite por IP e obrigatorio: sem os dois, esta rota vira uma listagem de tutores com
 * telefone, consultavel por quem tiver paciencia para contar ate 999999999999999.
 */
@RestController
@RequestMapping("/found")
@RequiredArgsConstructor
public class FoundAnimalController {

    private final FoundAnimalService foundAnimalService;

    @Operation(summary = "Procura um animal pelo numero do microchip",
               description = "PUBLICA e sem conta, de proposito: e usada por quem encontrou o "
                             + "animal na rua e pode ser a unica pessoa com ele nas proximas "
                             + "horas. Devolve o cartao de emergencia inteiro — contatos, "
                             + "alergias, condicoes, medicacao em curso e vacinacao — e nada "
                             + "alem: historico clinico, diagnostico e endereco continuam fora. "
                             + "O acesso fica registrado no log que o tutor le, sem identificar "
                             + "quem buscou. Limitada por IP contra varredura de numeros. "
                             + "404 com codigo proprio quando o numero nao esta no Petfy.")
    @PostMapping
    public ResponseEntity<FoundAnimalCardDTO> procurar(@Valid @RequestBody FoundAnimalRequestDTO dto) {
        return ResponseEntity.ok(foundAnimalService.procurar(dto.getMicrochipNumber()));
    }

}
