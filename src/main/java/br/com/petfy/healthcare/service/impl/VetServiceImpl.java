package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.VetRequestDTO;
import br.com.petfy.healthcare.domain.dto.VetResponseDTO;
import br.com.petfy.healthcare.domain.entity.Clinic;
import br.com.petfy.healthcare.domain.entity.Vet;
import br.com.petfy.healthcare.domain.repository.ClinicRepository;
import br.com.petfy.healthcare.domain.repository.OwnerRepository;
import br.com.petfy.healthcare.domain.repository.VetRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentVetProvider;
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
    private final OwnerRepository ownerRepository;
    private final ClinicService clinicService;
    private final PasswordEncoder passwordEncoder;
    private final CurrentVetProvider currentVetProvider;

    @Transactional
    @Override
    public VetResponseDTO register(VetRequestDTO request) {
        garantirEmailLivre(request.getEmail());

        Clinic clinic = resolverClinica(request);

        Vet vet = vetRepository.save(Vet.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .crmv(request.getCrmv())
                .clinic(clinic)
                .creationDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .build());

        return toResponse(vet);
    }

    @Override
    public VetResponseDTO getCurrentVet() {
        return toResponse(currentVetProvider.require());
    }

    /**
     * Owner e vet vivem em tabelas separadas, entao a unicidade de email entre
     * as duas nao e garantida pelo banco. Sem esta checagem, o mesmo email nos
     * dois lados tornaria o login ambiguo.
     */
    private void garantirEmailLivre(String email) {
        boolean jaUsado = vetRepository.existsByEmail(email) || ownerRepository.findByEmail(email).isPresent();

        if (jaUsado) {
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.EMAIL_ALREADY_USED.getMessage(),
                    ErrorMessageEnum.EMAIL_ALREADY_USED.getCode(),
                    HttpStatus.CONFLICT);
        }
    }

    /**
     * O primeiro veterinario cadastra a clinica junto; os proximos entram
     * informando o clinicId. Nao ha aprovacao de quem ja esta na clinica - ver
     * limitacoes no README.
     */
    private Clinic resolverClinica(VetRequestDTO request) {
        boolean temId = request.getClinicId() != null;
        boolean temDados = request.getClinic() != null;

        if (temId == temDados) {
            throw new PetfyHealthcareException(
                    "informe clinicId para entrar numa clinica existente, ou clinic para cadastrar uma nova",
                    ErrorMessageEnum.INVALID_REQUEST.getCode(),
                    HttpStatus.BAD_REQUEST);
        }

        return temId ? buscarClinica(request.getClinicId()) : criarClinica(request);
    }

    private Clinic buscarClinica(UUID clinicId) {
        return clinicRepository.findById(clinicId)
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.CLINIC_NOT_FOUND.getMessage(),
                        ErrorMessageEnum.CLINIC_NOT_FOUND.getCode(),
                        HttpStatus.NOT_FOUND));
    }

    private Clinic criarClinica(VetRequestDTO request) {
        UUID clinicId = clinicService.createClinic(request.getClinic()).getClinicId();
        return buscarClinica(clinicId);
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
