package br.com.petfy.healthcare.service.enums;

import lombok.Getter;

@Getter
public enum ErrorMessageEnum {

    OWNER_NOT_FOUND(101, "Owner not found"),
    PET_NOT_FOUND(102, "Pet not found"),
    CLINIC_NOT_FOUND(103, "Clinic not found"),
    VACCINE_NOT_FOUND(104, "Vaccine not found");

    private final int code;
    private final String message;

    ErrorMessageEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

}
