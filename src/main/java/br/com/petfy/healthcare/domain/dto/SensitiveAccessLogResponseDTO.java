package br.com.petfy.healthcare.domain.dto;

import br.com.petfy.healthcare.domain.entity.AccessActorType;
import br.com.petfy.healthcare.domain.entity.AccessedResource;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Um acesso, como o tutor o ve.
 *
 * O IP <b>volta</b> nesta resposta, ao contrario do que acontece no consentimento.
 * A diferenca e de quem e o dado: la o IP era do proprio titular consultando o
 * proprio aceite, e devolve-lo nao acrescentava nada; aqui e a evidencia de um
 * terceiro que abriu a carteira do animal dele, e sem ela um acesso por link publico
 * fica indistinguivel do outro. E o unico jeito de o tutor decidir se revoga o link.
 *
 * O user agent nao volta: e ruido para quem le, e nao muda decisao nenhuma.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensitiveAccessLogResponseDTO {

    private UUID sensitiveAccessLogId;

    private UUID animalId;

    private AccessActorType actorType;

    /** Nome de quem acessou, ou nulo quando foi pelo link publico. */
    private String actorName;

    private String clinicName;

    private AccessedResource resource;

    private LocalDateTime accessedAt;

    private String ipAddress;

}
