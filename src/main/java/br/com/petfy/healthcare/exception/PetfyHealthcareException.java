package br.com.petfy.healthcare.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class PetfyHealthcareException extends RuntimeException {

    private final HttpStatus httpStatus;

    public PetfyHealthcareException(String messsage, HttpStatus httpStatus) {
        super(messsage);
        this.httpStatus = httpStatus;
    }

}
