package br.com.petfy.healthcare.security;

public enum UserRole {

    /** Tutor: dono dos pets. */
    OWNER,

    /** Veterinario, vinculado a uma clinica. */
    VET;

    /** O Spring Security espera o prefixo ROLE_ para hasRole funcionar. */
    public String asAuthority() {
        return "ROLE_" + name();
    }

}
