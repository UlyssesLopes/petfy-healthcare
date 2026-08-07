package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.CareNetworkMemberDTO;
import br.com.petfy.healthcare.service.CareNetworkService;
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
 * A rede de quem cuida (DESIGN 5.4).
 *
 * <b>"As pessoas em volta do animal vem antes dos dados"</b>, e essa posicao e a tese da
 * secao 1 do PRODUTO virando tela: o registro e o fio que liga quem cuida. Esta rota existe
 * para a tela poder cumprir isso numa leitura - antes dela o cliente juntaria tres rotas,
 * e nenhuma das tres trazia a ultima contribuicao.
 */
@RestController
@RequestMapping("/animals/{animalId}/care-network")
@RequiredArgsConstructor
public class CareNetworkController {

    private final CareNetworkService careNetworkService;

    @Operation(summary = "Quem alcanca este animal, e quando contribuiu por ultimo",
               description = "Quem responde pelo animal vem primeiro. Traz custodia e concessao, de "
                             + "pessoa e de organizacao, com escopo e ultima contribuicao. Nao traz "
                             + "e-mail nem telefone - e quem tem alcance de fato, nao uma lista de "
                             + "contatos. Nao traz link de compartilhamento: link e alcance anonimo, "
                             + "tem rota propria, e o token dele e a credencial. Concessao revogada "
                             + "ou vencida nao aparece.")
    @GetMapping
    public ResponseEntity<List<CareNetworkMemberDTO>> doAnimal(@PathVariable UUID animalId) {
        return ResponseEntity.ok(careNetworkService.doAnimal(animalId));
    }

}
