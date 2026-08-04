package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.OwnerRequestDTO;
import br.com.petfy.healthcare.domain.dto.OwnerResponseDTO;
import br.com.petfy.healthcare.domain.dto.PasswordChangeRequestDTO;
import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.repository.OwnerRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentOwnerProvider;
import br.com.petfy.healthcare.service.EmailVerificationService;
import br.com.petfy.healthcare.service.OwnerService;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OwnerServiceImpl implements OwnerService {

    private final OwnerRepository ownerRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentOwnerProvider currentOwnerProvider;
    private final EmailVerificationService emailVerificationService;

    @Override
    public OwnerResponseDTO createOwner(OwnerRequestDTO request) {

        Owner owner = Owner.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .address(request.getAddress())
                .creationDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .build();

        Owner salvo = ownerRepository.save(owner);

        // e-mail comeca sem verificacao: a conta funciona, mas nao recebe aviso
        // ate o tutor confirmar o endereco. Falha de envio aqui nao desfaz o
        // cadastro - quem nao receber pede o reenvio
        emailVerificationService.sendVerification(salvo);

        return toResponseDTO(salvo);
    }

    @Override
    public OwnerResponseDTO getCurrentOwner() {
        return toResponseDTO(currentOwnerProvider.require());
    }

    @Override
    public OwnerResponseDTO updateCurrentOwner(OwnerRequestDTO request) {
        Owner existingOwner = currentOwnerProvider.require();

        if (request.getName() != null) {
            existingOwner.setName(request.getName());
        }

        if (request.getEmail() != null) {
            existingOwner.setEmail(request.getEmail());
        }

        if (request.getPhone() != null) {
            existingOwner.setPhone(request.getPhone());
        }

        if (request.getAddress() != null) {
            existingOwner.setAddress(request.getAddress());
        }

        // password fica de fora de proposito: troca de senha pede endpoint
        // proprio, com confirmacao da senha atual
        existingOwner.setUpdateDate(LocalDateTime.now());

        return toResponseDTO(ownerRepository.save(existingOwner));
    }

    @Override
    public void changePassword(PasswordChangeRequestDTO request) {
        Owner owner = currentOwnerProvider.require();

        // exigir a senha atual e o que impede que um token roubado, sozinho,
        // troque a senha e tome a conta em definitivo
        if (!passwordEncoder.matches(request.getCurrentPassword(), owner.getPassword())) {
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.CURRENT_PASSWORD_DOES_NOT_MATCH.getMessage(),
                    ErrorMessageEnum.CURRENT_PASSWORD_DOES_NOT_MATCH.getCode(),
                    HttpStatus.BAD_REQUEST);
        }

        // sem isso, quem troca a senha depois de um vazamento acha que rodou a
        // credencial quando na pratica nao mudou nada
        if (passwordEncoder.matches(request.getNewPassword(), owner.getPassword())) {
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.NEW_PASSWORD_MUST_DIFFER.getMessage(),
                    ErrorMessageEnum.NEW_PASSWORD_MUST_DIFFER.getCode(),
                    HttpStatus.BAD_REQUEST);
        }

        owner.setPassword(passwordEncoder.encode(request.getNewPassword()));

        // e isto que derruba as sessoes abertas: o filtro recusa token emitido
        // antes deste instante. Sem o carimbo, trocar a senha nao expulsaria
        // quem ja estava dentro, que e justamente o motivo de trocar
        owner.setPasswordChangedAt(LocalDateTime.now());
        owner.setUpdateDate(LocalDateTime.now());
        ownerRepository.save(owner);
    }

    @Override
    public void deleteCurrentOwner() {
        ownerRepository.delete(currentOwnerProvider.require());
    }

    private OwnerResponseDTO toResponseDTO(Owner owner) {
        return OwnerResponseDTO.builder()
                .ownerId(owner.getOwnerId())
                .name(owner.getName())
                .email(owner.getEmail())
                .phone(owner.getPhone())
                .address(owner.getAddress())
                .creationDate(owner.getCreationDate())
                .updateDate(owner.getUpdateDate())
                .build();
    }

}
