package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.ServiceAppointmentCloseRequestDTO;
import br.com.petfy.healthcare.domain.dto.ServiceAppointmentRequestDTO;
import br.com.petfy.healthcare.domain.dto.ServiceAppointmentResponseDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** A agenda de banho e tosa (Tela 18). */
public interface ServiceAppointmentService {

    /** "Segunda, 10 de agosto · 8 banhos hoje." */
    List<ServiceAppointmentResponseDTO> doDia(LocalDate dia);

    ServiceAppointmentResponseDTO agendar(ServiceAppointmentRequestDTO dto);

    /** "Marcar entrada." */
    ServiceAppointmentResponseDTO marcarEntrada(UUID appointmentId);

    /** "Entregar e avisar Marcelo." O texto, quando houver, vira observacao assinada. */
    ServiceAppointmentResponseDTO entregar(UUID appointmentId,
                                           ServiceAppointmentCloseRequestDTO dto);

    /** O animal nao veio. A ausencia e informacao, como a falta na creche. */
    ServiceAppointmentResponseDTO faltou(UUID appointmentId);

}
