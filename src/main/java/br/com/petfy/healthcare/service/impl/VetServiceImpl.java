package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.VetRequestDTO;
import br.com.petfy.healthcare.domain.dto.VetResponseDTO;
import br.com.petfy.healthcare.domain.entity.Clinic;
import br.com.petfy.healthcare.domain.entity.ClinicInvite;
import br.com.petfy.healthcare.domain.entity.Vet;
import br.com.petfy.healthcare.domain.repository.ClinicRepository;
import br.com.petfy.healthcare.domain.repository.PersonRepository;
import br.com.petfy.healthcare.domain.repository.VetRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentVetProvider;
import br.com.petfy.healthcare.service.ClinicInviteService;
import br.com.petfy.healthcare.service.ClinicService;
import br.com.petfy.healthcare.service.VetService;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VetServiceImpl implements VetService {

    private final VetRepository vetRepository;
    private final ClinicRepository clinicRepository;
    private final PersonRepository personRepository;
    private final ClinicService clinicService;
    private final ClinicInviteService clinicInviteService;
    private final PasswordEncoder passwordEncoder;
    private final CurrentVetProvider currentVetProvider;

    /**
     * Duas portas de entrada, e nenhuma delas aceita simplesmente apontar para
     * uma clinica existente: ou o vet cadastra a clinica e vira o primeiro dela,
     * ou apresenta um convite emitido por quem ja esta la.
     */
    @Transactional
    @Override
    public VetResponseDTO register(VetRequestDTO request) {
        garantirEmailLivre(request.getEmail());

        boolean temConvite = request.getInviteToken() != null && !request.getInviteToken().isBlank();
        boolean temClinicaNova = request.getClinic() != null;

        if (temConvite == temClinicaNova) {
            throw new PetfyHealthcareException(
                    "informe inviteToken para entrar numa clinica existente, ou clinic para cadastrar uma nova",
                    ErrorMessageEnum.INVALID_REQUEST.getCode(),
                    HttpStatus.BAD_REQUEST);
        }

        // valida antes de criar o vet: nao faz sentido gravar a conta para depois
        // descobrir que o convite nao servia
        ClinicInvite invite = temConvite
                ? clinicInviteService.validate(request.getInviteToken(), request.getEmail())
                : null;

        Clinic clinic = invite != null ? invite.getClinic() : criarClinica(request);

        Vet vet = vetRepository.save(Vet.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .crmv(request.getCrmv())
                .clinic(clinic)
                .creationDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .build());

        // consome o convite: e de uso unico, senao o mesmo link serviria a
        // qualquer numero de pessoas
        if (invite != null) {
            clinicInviteService.markAccepted(invite, vet.getVetId());
        }

        return toResponse(vet);
    }

    @Override
    public VetResponseDTO getCurrentVet() {
        return toResponse(currentVetProvider.require());
    }

    /**
     * Person e vet vivem em tabelas separadas, entao a unicidade de email entre
     * as duas nao e garantida pelo banco. Sem esta checagem, o mesmo email nos
     * dois lados tornaria o login ambiguo.
     */
    private void garantirEmailLivre(String email) {
        boolean jaUsado = vetRepository.existsByEmail(email) || personRepository.findByEmail(email).isPresent();

        if (jaUsado) {
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.EMAIL_ALREADY_USED.getMessage(),
                    ErrorMessageEnum.EMAIL_ALREADY_USED.getCode(),
                    HttpStatus.CONFLICT);
        }
    }

    private Clinic criarClinica(VetRequestDTO request) {
        UUID clinicId = clinicService.createClinic(request.getClinic()).getClinicId();

        return clinicRepository.findById(clinicId)
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.CLINIC_NOT_FOUND.getMessage(),
                        ErrorMessageEnum.CLINIC_NOT_FOUND.getCode(),
                        HttpStatus.NOT_FOUND));
    }

    private VetResponseDTO toResponse(Vet vet) {
        return VetResponseDTO.builder()
                .vetId(vet.getVetId())
                .name(vet.getName())
                .email(vet.getEmail())
                .crmv(vet.getCrmv())
                .clinicId(vet.getClinic().getClinicId())
                .clinicName(vet.getClinic().getName())
                .creationDate(vet.getCreationDate())
                .updateDate(vet.getUpdateDate())
                .build();
    }

}
