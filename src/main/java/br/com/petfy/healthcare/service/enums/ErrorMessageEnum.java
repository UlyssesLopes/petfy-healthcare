package br.com.petfy.healthcare.service.enums;

import lombok.Getter;

@Getter
public enum ErrorMessageEnum {

    OWNER_NOT_FOUND(101, "Owner not found"),
    PET_NOT_FOUND(102, "Pet not found"),
    CLINIC_NOT_FOUND(103, "Clinic not found"),
    VACCINE_NOT_FOUND(104, "Vaccine not found"),
    HEALTH_RECORD_NOT_FOUND(105, "Health record not found"),
    VACCINE_CATALOG_NOT_FOUND(106, "Vaccine catalog entry not found"),
    SHARE_NOT_FOUND(107, "Share link not found or no longer valid"),
    EMAIL_ALREADY_USED(108, "Email already registered"),
    NOT_CLINIC_MEMBER(109, "Only a vet from this clinic can do that"),
    CLINIC_ACCESS_NOT_FOUND(110, "Clinic access not found"),
    INVITE_NOT_FOUND(111, "Invite not found or no longer valid"),
    CORRECTION_WINDOW_EXPIRED(112, "Correction window for this record has expired"),
    CURRENT_PASSWORD_DOES_NOT_MATCH(113, "Current password does not match"),
    NEW_PASSWORD_MUST_DIFFER(114, "New password must be different from the current one"),
    // mensagem deliberadamente vaga: nao distingue inexistente, expirado e ja usado
    RESET_TOKEN_NOT_FOUND(115, "Reset token not found or no longer valid"),
    VERIFICATION_TOKEN_NOT_FOUND(116, "Verification token not found or no longer valid"),
    // vale para o catalogo de vacina e para o de antiparasitario, por isso a
    // mensagem nao nomeia mais so a vacina. O codigo 117 nao mudou
    SPECIES_MISMATCH(117, "Catalog species does not match the pet species"),
    ANTIPARASITIC_CATALOG_NOT_FOUND(118, "Antiparasitic catalog entry not found"),
    // 403, e nao 404: so chega aqui quem ja e tutor do pet, entao a resposta nao
    // revela a existencia de nada que a pessoa ainda nao conhecesse
    INSUFFICIENT_PET_ROLE(119, "Your role on this pet does not allow this action"),
    PET_TUTOR_INVITE_NOT_FOUND(120, "Invite not found or no longer valid"),
    ALREADY_A_TUTOR(121, "This person is already a tutor of this pet"),
    CANNOT_REMOVE_HOLDER(122, "The holder cannot be removed; transfer ownership first"),
    // promover alguem a titular rebaixa o titular atual, entao nao cabe no PATCH
    // de papel: e a transferencia, que tem endpoint proprio
    TRANSFER_REQUIRED_FOR_HOLDER(123, "Use the ownership transfer endpoint to change the holder"),
    TUTOR_NOT_FOUND(124, "This person is not a tutor of this pet"),
    INVALID_REQUEST(400, "Invalid request"),
    INVALID_CREDENTIALS(401, "Invalid email or password");

    private final int code;
    private final String message;

    ErrorMessageEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

}
