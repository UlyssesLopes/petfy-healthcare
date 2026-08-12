package br.com.petfy.healthcare.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * O numero digitado por quem achou um animal na rua (Tela 34).
 *
 * <b>POST, e nao GET com o numero na query — e a razao e concreta.</b> O {@code RateLimitFilter}
 * so intercepta POST, e uma rota publica que devolve nome e telefone a partir de um numero nao
 * pode ficar sem trava contra varredura. Alem disso, um GET poria o microchip no log de acesso do
 * servidor e no historico do navegador de quem buscou, que e o oposto de "nao guardamos quem fez
 * a busca".
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoundAnimalRequestDTO {

    /**
     * O numero inteiro, e nao um pedaco.
     *
     * <b>Busca parcial nao existe aqui de proposito:</b> "9810" acharia todo animal de uma
     * fabricante de chip, e transformaria esta rota numa listagem de tutores com telefone. Quem
     * tem o animal na frente tem o numero inteiro — e o leitor de microchip devolve os 15
     * digitos.
     */
    @NotBlank(message = "microchipNumber e obrigatorio")
    @Size(max = 32, message = "microchipNumber nao pode passar de 32 caracteres")
    private String microchipNumber;

}
