package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.LoginRequestDTO;
import br.com.petfy.healthcare.domain.dto.LoginResponseDTO;
import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.repository.OwnerRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.JwtService;
import br.com.petfy.healthcare.service.AuthService;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final OwnerRepository ownerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public LoginResponseDTO login(LoginRequestDTO request) {
        Optional<Owner> encontrado = ownerRepository.findByEmail(request.getEmail());

        // a mesma resposta para email inexistente e para senha errada: distinguir
        // os dois casos entregaria de graca quais emails estao cadastrados
        Owner owner = encontrado
                .filter(o -> passwordEncoder.matches(request.getPassword(), o.getPassword()))
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.INVALID_CREDENTIALS.getMessage(),
                        ErrorMessageEnum.INVALID_CREDENTIALS.getCode(),
                        HttpStatus.UNAUTHORIZED));

        return LoginResponseDTO.builder()
                .token(jwtService.generateToken(owner))
                .tokenType("Bearer")
                .expiresInMinutes(jwtService.getExpirationMinutes())
                .ownerId(owner.getOwnerId())
                .build();
    }

}
