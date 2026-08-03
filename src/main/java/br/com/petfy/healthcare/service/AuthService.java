package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.LoginRequestDTO;
import br.com.petfy.healthcare.domain.dto.LoginResponseDTO;

public interface AuthService {

    LoginResponseDTO login(LoginRequestDTO request);

}
