package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.PersonRequestDTO;
import br.com.petfy.healthcare.domain.dto.PersonResponseDTO;
import br.com.petfy.healthcare.domain.dto.PasswordChangeRequestDTO;

/**
 * Nao ha busca por id nem listagem: um person so enxerga a si mesmo, entao o id
 * viria sempre do token e nunca da URL.
 */
public interface PersonService {

    PersonResponseDTO createPerson(PersonRequestDTO request);

    PersonResponseDTO getCurrentPerson();

    PersonResponseDTO updateCurrentPerson(PersonRequestDTO request);

    /**
     * Endpoint proprio porque o updateCurrentPerson ignora o campo password: trocar
     * senha exige confirmar a atual, e um PUT parcial nao tem como exigir isso.
     */
    void changePassword(PasswordChangeRequestDTO request);

    /**
     * "Sua conta" (Tela 36): o estado da conta, e o que falta para poder encerra-la.
     */
    br.com.petfy.healthcare.domain.dto.AccountOverviewDTO conta();

    /**
     * "Registro profissional · Declarar" (Tela 36).
     *
     * <b>So dava para declarar no cadastro ate aqui</b>, e a veterinaria que criou a conta como tutora
     * — porque descobriu o Petfy pelo proprio cachorro — nao tinha como dizer depois que e
     * veterinaria. Ela criaria uma segunda conta, sem nenhum dos animais que ja acompanha.
     */
    br.com.petfy.healthcare.domain.dto.AccountOverviewDTO declararCredencial(
            br.com.petfy.healthcare.domain.dto.ProfessionalCredentialRequestDTO request);

    /**
     * Encerra a conta.
     *
     * <b>RECUSA enquanto houver animal sob a responsabilidade da pessoa</b> (PRODUTO 3.4, Tela 36).
     * Nenhum animal morre com a conta, e nenhuma responsabilidade passa a quem nao disse sim.
     */
    void deleteCurrentPerson();

}
