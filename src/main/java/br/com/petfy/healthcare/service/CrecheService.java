package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.AttendanceRequestDTO;
import br.com.petfy.healthcare.domain.dto.AttendanceResponseDTO;
import br.com.petfy.healthcare.domain.dto.ClassGroupRequestDTO;
import br.com.petfy.healthcare.domain.dto.ClassGroupResponseDTO;
import br.com.petfy.healthcare.domain.dto.EnrollmentAgreementRequestDTO;
import br.com.petfy.healthcare.domain.dto.EnrollmentResponseDTO;
import br.com.petfy.healthcare.domain.dto.VaccineRequirementRequestDTO;
import br.com.petfy.healthcare.domain.dto.VaccineRequirementResponseDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * A operacao da creche: turma, vaga, matricula e o dia.
 *
 * <b>E o que estava atras da capacidade {@code GERIR_TURMA_E_VAGA}</b>, declarada e vazia desde o
 * P3. Duas telas da entrega dependem inteira dele: a 10 ("matricula na creche — o produto responde
 * pela saude") e a 17, que o desenho chama de "a tela mais usada da creche".
 *
 * <b>Toda operacao aqui exige organizacao declarada no cabecalho.</b> Nao e rigor: uma monitora que
 * trabalha em duas creches marcaria entrada na turma errada, e o animal apareceria como presente
 * num lugar onde nao esta.
 */
public interface CrecheService {

    List<ClassGroupResponseDTO> listClassGroups();

    ClassGroupResponseDTO createClassGroup(ClassGroupRequestDTO request);

    List<VaccineRequirementResponseDTO> listRequirements();

    VaccineRequirementResponseDTO addRequirement(VaccineRequirementRequestDTO request);

    void removeRequirement(UUID requirementId);

    /**
     * Matricula o animal na turma.
     *
     * <b>Nunca recusa por saude.</b> A matricula nasce PENDENTE quando a comprovacao nao fecha, e o
     * proprio produto a completa depois — "a matricula fica guardada e se completa sozinha assim que
     * a dose for registrada". Recusar faria a creche refazer o cadastro, e faria o tutor achar que o
     * problema e com ele.
     */
    EnrollmentResponseDTO enroll(UUID animalId, UUID classGroupId);

    /** As matriculas da turma, com a comprovacao de cada uma reavaliada agora. */
    List<EnrollmentResponseDTO> listEnrollments(UUID classGroupId);

    /**
     * Grava "o combinado com o tutor" (Tela 41): mensalidade, vencimento, diaria e os dias.
     *
     * <b>Substitui o combinado inteiro.</b> Combinar e um ato unico, e quem renegocia diz de novo o
     * que passou a valer — o que ele nao repetir deixou de valer.
     *
     * <b>Nao lanca cobranca nenhuma.</b> "O Petfy nao cobra, nao emite boleto e nao processa
     * pagamento. Ele guarda o que foi combinado." A mensalidade e um valor guardado; a unica coisa
     * que vira lancamento sozinha e a diaria, e ela nasce do check-in.
     */
    EnrollmentResponseDTO setAgreement(UUID enrollmentId, EnrollmentAgreementRequestDTO request);

    /** A matricula de um animal, do lado do tutor: e a Tela 10 vista por quem concedeu acesso. */
    List<EnrollmentResponseDTO> listEnrollmentsOfAnimal(UUID animalId);

    /** O dia da turma: quem e esperado, quem chegou, quem saiu, quem faltou. */
    List<AttendanceResponseDTO> listDay(UUID classGroupId, LocalDate day);

    AttendanceResponseDTO checkIn(UUID enrollmentId, AttendanceRequestDTO request);

    AttendanceResponseDTO checkOut(UUID enrollmentId);

    AttendanceResponseDTO markAbsence(UUID enrollmentId);
}
