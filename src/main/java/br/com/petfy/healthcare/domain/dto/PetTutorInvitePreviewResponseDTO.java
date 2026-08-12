package br.com.petfy.healthcare.domain.dto;

import br.com.petfy.healthcare.domain.entity.PetTutorRole;
import lombok.*;

import java.time.LocalDateTime;

/**
 * O convite ANTES de aceitar (Telas 19, 20 e 21).
 *
 * <b>Ler nao consome.</b> É a mesma regra do convite de organização, e pela mesma razão: abrir o link
 * para entender o que está sendo oferecido não pode gastar o direito de entrar. Quem clica num link do
 * e-mail no ônibus e fecha o app tem de poder voltar depois.
 *
 * <b>E aceitar às cegas não é aceitar.</b> O que está em jogo aqui é maior que numa equipe: ou a
 * pessoa passa a ver a saúde inteira de um animal, ou ela passa a RESPONDER por um animal. As três
 * telas do desenho são a mesma mecânica com três significados, e é o {@link #role} que diz qual.
 *
 * <b>O que NÃO viaja: nada de saúde.</b> Quem ainda não aceitou não alcança o animal, e a tela precisa
 * só do nome dele, de quem convidou e do que está sendo oferecido. Mandar condição, vacina ou peso
 * aqui entregaria o prontuário a quem tem um link — que é o oposto do que o convite existe para
 * proteger.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetTutorInvitePreviewResponseDTO {

    private String animalName;

    /** Quem convidou. "Marcelo quer dividir o cuidado do Code com você." */
    private String invitedByName;

    /**
     * O que está sendo oferecido.
     *
     * {@code HOLDER} significa passar a responder pelo animal — a Tela 20 quando vem de uma pessoa, e
     * a Tela 21 quando vem de um abrigo. Os outros papéis são dividir o cuidado, que é a Tela 19.
     */
    private PetTutorRole role;

    /**
     * Se quem responde pelo animal hoje é uma organização.
     *
     * <b>É o que separa a Tela 20 da 21</b> — "Marcelo quer passar o Code para você" contra "o abrigo
     * quer que você adote". A mecânica é idêntica; a frase não pode ser, porque adotar e receber de um
     * amigo são coisas diferentes para quem lê.
     */
    private boolean fromOrganization;

    /** O nome de quem responde hoje: a pessoa ou o abrigo. */
    private String currentHolderName;

    private LocalDateTime expiresAt;

}
