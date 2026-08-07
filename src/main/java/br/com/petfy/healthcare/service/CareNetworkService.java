package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.CareNetworkMemberDTO;

import java.util.List;
import java.util.UUID;

/**
 * A rede de quem cuida do animal (DESIGN 5.4).
 *
 * <b>Uma leitura, e nao tres.</b> Antes disto o cliente juntaria
 * {@code /animals/{id}/tutors}, {@code /animals/{id}/shares} e
 * {@code /animals/{id}/organization-access} por conta propria - e o argumento e o mesmo
 * que a 9.5 usa para o feed de pendencias: se o cliente monta a regra, <b>cada cliente
 * novo remonta e erra diferente</b>. O app seria o segundo a errar.
 */
public interface CareNetworkService {

    /**
     * Quem alcanca este animal agora, quem responde por ele primeiro.
     *
     * Nao inclui link de compartilhamento: link e alcance anonimo, e nao alguem que cuida.
     * Ele tem rota propria, e o token dele e a credencial - devolve-lo aqui seria entregar
     * a chave numa leitura de tela.
     */
    List<CareNetworkMemberDTO> doAnimal(UUID animalId);

}
