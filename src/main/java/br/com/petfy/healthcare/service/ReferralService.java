package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.ReferralCandidateDTO;
import br.com.petfy.healthcare.domain.dto.ReferralOptionsDTO;
import br.com.petfy.healthcare.domain.dto.ReferralRequestDTO;
import br.com.petfy.healthcare.domain.dto.ReferralResponseDTO;

import java.util.List;
import java.util.UUID;

/** O caso que passa adiante (Tela 45). */
public interface ReferralService {

    /** O que pode ir junto, com o que existe de verdade em cada escopo. */
    ReferralOptionsDTO opcoes(UUID animalId);

    /** "Buscar outro profissional." */
    List<ReferralCandidateDTO> candidatos(UUID animalId, String busca);

    ReferralResponseDTO encaminhar(UUID animalId, ReferralRequestDTO dto);

    /** O que esta clinica encaminhou deste animal, e no que deu. */
    List<ReferralResponseDTO> doAnimal(UUID animalId);

    /** O que espera decisao de quem responde pelos animais de quem esta lendo. */
    List<ReferralResponseDTO> pendentesParaDecisao();

    /** O que chegou para quem esta lendo — a caixa do especialista. */
    List<ReferralResponseDTO> recebidos();

    ReferralResponseDTO autorizar(UUID referralId);

    ReferralResponseDTO recusar(UUID referralId);

}
