package br.com.petfy.healthcare.service.enums;

import lombok.Getter;

@Getter
public enum ErrorMessageEnum {

    PERSON_NOT_FOUND(101, "Person not found"),
    ANIMAL_NOT_FOUND(102, "Animal not found"),
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
    SPECIES_MISMATCH(117, "Catalog species does not match the animal species"),
    ANTIPARASITIC_CATALOG_NOT_FOUND(118, "Antiparasitic catalog entry not found"),
    // 403, e nao 404: so chega aqui quem ja e tutor do animal, entao a resposta nao
    // revela a existencia de nada que a pessoa ainda nao conhecesse
    INSUFFICIENT_ANIMAL_ROLE(119, "Your role on this animal does not allow this action"),
    PET_TUTOR_INVITE_NOT_FOUND(120, "Invite not found or no longer valid"),
    ALREADY_A_TUTOR(121, "This person is already a tutor of this animal"),
    CANNOT_REMOVE_HOLDER(122, "The holder cannot be removed; transfer personship first"),
    // promover alguem a titular rebaixa o titular atual, entao nao cabe no PATCH
    // de papel: e a transferencia, que tem endpoint proprio
    TRANSFER_REQUIRED_FOR_HOLDER(123, "Use the personship transfer endpoint to change the holder"),
    TUTOR_NOT_FOUND(124, "This person is not a tutor of this animal"),
    ATTACHMENT_NOT_FOUND(125, "Attachment not found"),
    // 415: o formato foi recusado pelo CONTEUDO, e nao pelo Content-Type declarado
    ATTACHMENT_TYPE_NOT_ALLOWED(126, "Attachment must be a JPEG, PNG, WEBP or PDF file"),
    ATTACHMENT_TOO_LARGE(127, "Attachment exceeds the maximum allowed size"),
    ATTACHMENT_STORAGE_FAILURE(128, "Could not store or read the attachment"),
    ATTACHMENT_EMPTY(129, "Attachment file is empty"),
    CONDITION_NOT_FOUND(130, "Health condition not found"),
    SEVERITY_ONLY_FOR_ALLERGY(131, "Severity applies to allergies only"),
    // trocar o tipo nao e corrigir um campo, e dizer que era outra coisa desde o comeco
    CONDITION_KIND_IS_IMMUTABLE(132, "The condition kind cannot be changed; create a new record instead"),
    INVALID_REQUEST(400, "Invalid request"),
    INVALID_CREDENTIALS(401, "Invalid email or password");

    private final int code;
    private final String message;

    ErrorMessageEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

}
