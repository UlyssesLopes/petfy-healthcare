package br.com.petfy.healthcare.domain.dto;

public enum VaccineStatus {

    /** A proxima dose ja passou da data. */
    OVERDUE,

    /** A proxima dose cai dentro da janela consultada. */
    DUE_SOON,

    /** Tem proxima dose marcada, mas fora da janela. */
    UP_TO_DATE,

    /** Sem proxima dose registrada - dose unica, ou o tutor nao preencheu. */
    NO_NEXT_DOSE

}
