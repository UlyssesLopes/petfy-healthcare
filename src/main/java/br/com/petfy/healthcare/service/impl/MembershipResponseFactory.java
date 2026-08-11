package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.MembershipResponseDTO;
import br.com.petfy.healthcare.domain.entity.Membership;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.ProfessionalCredential;
import br.com.petfy.healthcare.domain.repository.ProfessionalCredentialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * O vinculo virando resposta, num lugar so.
 *
 * <b>Nasceu de dois chamadores, e nao de antecipacao.</b> A equipe (Tela 16) lista vinculos e o
 * aceite de convite devolve o vinculo recem-criado; a segunda copia do mesmo mapeamento e que
 * teria feito a credencial aparecer numa tela e sumir na outra.
 *
 * <b>Quem chama precisa estar em transacao.</b> O DTO le {@code person.getName()} e
 * {@code organization.getName()} de entidade vinda do repositorio, e montar DTO a partir de
 * entidade fora de transacao estoura o proxy — o defeito que ja custou cinco 500 neste projeto.
 */
@Component
@RequiredArgsConstructor
public class MembershipResponseFactory {

    private final ProfessionalCredentialRepository professionalCredentialRepository;

    public MembershipResponseDTO toResponse(Membership vinculo) {
        Person pessoa = vinculo.getPerson();

        return MembershipResponseDTO.builder()
                .membershipId(vinculo.getMembershipId())
                .organizationId(vinculo.getOrganization().getOrganizationId())
                .organizationName(vinculo.getOrganization().getName())
                .personId(pessoa.getPersonId())
                .personName(pessoa.getName())
                .personEmail(pessoa.getEmail())
                .role(vinculo.getRole())
                .joinedAt(vinculo.getJoinedAt())
                .professionalCredential(credencialDe(pessoa))
                .build();
    }

    /**
     * "CRMV-SP 12345", quando existe. A ausencia e o normal do monitor e do voluntario, e por isso
     * ela e nula em vez de ser um vazio que a tela teria de explicar.
     */
    private String credencialDe(Person pessoa) {
        return professionalCredentialRepository.findByPersonPersonId(pessoa.getPersonId())
                .stream()
                .findFirst()
                .map(this::formatar)
                .orElse(null);
    }

    private String formatar(ProfessionalCredential credencial) {
        return credencial.getCouncil() + "-" + credencial.getUf() + " " + credencial.getNumber();
    }

}
