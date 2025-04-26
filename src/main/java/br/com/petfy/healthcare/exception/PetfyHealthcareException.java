package br.com.petfy.healthcare.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class PetfyHealthcareException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final int code;

    public PetfyHealthcareException(String messsage, int code, HttpStatus httpStatus) {
        super(messsage);
        this.code = code;
        this.httpStatus = httpStatus;
    }

}
