package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.SponsoredEventDTO;
import br.com.petfy.healthcare.domain.dto.SponsorshipOfferDTO;
import br.com.petfy.healthcare.domain.dto.SponsorshipRequestDTO;
import br.com.petfy.healthcare.domain.dto.SponsorshipResponseDTO;

import java.util.List;
import java.util.UUID;

/** Quem banca o cuidado de um animal de abrigo (Tela 46). */
public interface SponsorshipService {

    /** A tela de apadrinhar: quem ele e, o que custa por mes, e quantos ja bancam. */
    SponsorshipOfferDTO oferta(UUID animalId);

    SponsorshipResponseDTO apadrinhar(UUID animalId, SponsorshipRequestDTO dto);

    /** "O que voce banca" — a lista do padrinho. */
    List<SponsorshipResponseDTO> meus();

    /** "O que voce passa a receber" — os eventos de custo do que ele banca. */
    List<SponsoredEventDTO> eventos(UUID sponsorshipId);

    /** Parar, com os trinta dias que a tela promete. */
    SponsorshipResponseDTO encerrar(UUID sponsorshipId);

    /** Quem banca os animais da organizacao declarada — a lista do abrigo. */
    List<SponsorshipResponseDTO> daOrganizacao();

}
