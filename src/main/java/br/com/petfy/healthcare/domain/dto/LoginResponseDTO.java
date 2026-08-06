package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDTO {

    private String token;

    private String tokenType;

    private long expiresInMinutes;

    /** Sempre preenchido: nao ha mais dois tipos de conta com dois ids. */
    private UUID personId;

    /**
     * A pessoa tem credencial profissional ativa agora.
     *
     * <b>Substituiu o campo {@code role}</b>, que dizia OWNER ou VET e vinha de
     * qual tabela o e-mail estava. Nao e o mesmo campo com outro nome: papel era
     * exclusivo - quem era vet nao era tutor -, e isto nao e. A veterinaria que
     * tem cachorro e uma pessoa so, com credencial e com animais.
     *
     * O cliente usa isto para decidir se <i>oferece</i> a area profissional. Quem
     * autoriza continua sendo o servidor, a cada requisicao: esconder um botao
     * nao e controle de acesso.
     */
    private boolean professional;

}
