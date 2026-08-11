package br.com.petfy.healthcare.service.enums;

import lombok.Getter;

@Getter
public enum ErrorMessageEnum {

    PERSON_NOT_FOUND(101, "Person not found"),
    ANIMAL_NOT_FOUND(102, "Animal not found"),
    CLINIC_NOT_FOUND(103, "Organization not found"),
    VACCINE_NOT_FOUND(104, "Vaccine not found"),
    HEALTH_RECORD_NOT_FOUND(105, "Health record not found"),
    VACCINE_CATALOG_NOT_FOUND(106, "Vaccine catalog entry not found"),
    SHARE_NOT_FOUND(107, "Share link not found or no longer valid"),
    EMAIL_ALREADY_USED(108, "Email already registered"),
    NOT_CLINIC_MEMBER(109, "Only a vet from this organization can do that"),
    CLINIC_ACCESS_NOT_FOUND(110, "Organization access not found"),
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
    CANNOT_REMOVE_HOLDER(122, "The holder cannot be removed; transfer the holder role first"),
    // promover alguem a titular rebaixa o titular atual, entao nao cabe no PATCH
    // de papel: e a transferencia, que tem endpoint proprio
    TRANSFER_REQUIRED_FOR_HOLDER(123, "Use the holder transfer endpoint to change the holder"),
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
    // 403: a pessoa esta autenticada e existe; o que falta e capacidade. Substituiu
    // o 401 que o CurrentVetProvider dava quando o e-mail nao estava na tabela de
    // vets - com pessoa unica todo mundo e encontrado, e nao ha o que esconder de
    // alguem sobre a propria conta
    PROFESSIONAL_CREDENTIAL_REQUIRED(133, "An active professional credential is required for this action"),
    CREDENTIAL_ALREADY_REGISTERED(134, "This professional credential is already registered"),
    // 403: a pessoa esta autenticada e tem credencial, mas nao e membro da organizacao
    // que ela pediu como contexto. Nada a esconder - ela sabe que a organizacao existe,
    // porque informou o id
    // 403: a organizacao nao tem a capacidade que a operacao exige. Sem isto, creche e
    // abrigo na mesma tabela da clinica herdariam o direito de escrever no prontuario
    // 409: a operacao e da organizacao, e a pessoa esta atuando por si. Nao e falta de
    // permissao - e falta de contexto, e a diferenca importa para o cliente
    ORGANIZATION_CONTEXT_REQUIRED(138, "This action requires acting on behalf of an organization"),
    CAPABILITY_NOT_GRANTED(137, "This organization cannot perform this action"),
    NOT_ORGANIZATION_MEMBER(135, "You are not an active member of this organization"),
    // 409: mais de um vinculo ativo e nenhum contexto informado. Escolher em silencio
    // faria um ato clinico sair assinado por uma organizacao que a pessoa nao pretendia
    AMBIGUOUS_CONTEXT(136, "You act for more than one organization; inform X-Petfy-Organization"),
    CARE_INSTRUCTION_NOT_FOUND(139, "Care instruction not found"),
    // 409: a orientacao existe, e nao esta valendo no instante do cumprimento -
    // revogada, ou fora do prazo. Aceitar zeraria a pendencia de um tratamento que nao
    // esta mais em curso
    CARE_INSTRUCTION_NOT_IN_EFFECT(140, "This care instruction was not in effect at the informed time"),
    // so se silencia o que esta sendo cobrado de voce: a pendencia e derivada, entao nao ha
    // chave estrangeira que recuse um par de tipo e id inventado
    DUE_ITEM_NOT_FOUND(141, "There is no such pending item for you"),
    // o consentimento bloqueia o resto do produto; silencia-lo esconderia o bloqueio
    CONSENT_CANNOT_BE_SILENCED(142, "Pending consent cannot be silenced"),
    // corpo que o Jackson nao consegue ler: JSON truncado, aspas soltas, charset errado.
    // Nao e regra de negocio, e mesmo assim mora nesta faixa - porque o cliente traduz POR
    // CODIGO, e reusar INVALID_REQUEST faria a tela dizer "confira os campos marcados"
    // quando nao ha campo marcado nenhum. Quem manda corpo ilegivel raramente e o usuario:
    // e o cliente que montou a requisicao errado
    MALFORMED_REQUEST_BODY(143, "Request body is not readable JSON"),

    // ------------------------------------------------------ a operacao da creche (V33)
    //
    // Os tres sao 404/409 de negocio, e nenhum e falta de permissao: a turma que nao e desta
    // organizacao responde 404 pela mesma razao do animal — 403 confirmaria que aquele id
    // existe. Vaga cheia e comprovacao faltando sao conflito de estado, e as duas telas da
    // creche precisam distinguir uma da outra para dizer o que fazer em seguida.
    CLASS_GROUP_NOT_FOUND(144, "Class group not found"),
    CLASS_GROUP_FULL(145, "Class group has no free spot"),
    ENROLLMENT_HEALTH_PROOF_MISSING(146, "Enrollment health proof is incomplete"),
    ATTENDANCE_NOT_CHECKED_IN(147, "Attendance has no check-in for today"),
    // Requisicao multipart que chegou sem a parte do arquivo. E diferente de ATTACHMENT_EMPTY,
    // que e arquivo escolhido e vazio: aqui nao houve arquivo nenhum, e o conselho da tela muda
    // de "esse arquivo esta vazio" para "escolha um arquivo". Sem este codigo a excecao caia no
    // handler generico e voltava 500, dizendo que o servidor falhou quando quem errou foi quem
    // chamou — e o contrato declara a parte como obrigatoria.
    MISSING_FILE_PART(148, "Request is missing the file part"),
    // Dose ja registrada para o mesmo animal, no mesmo dia, da mesma vacina. O cenario nao e
    // hipotetico: a clinica registra a aplicacao e o tutor registra a mesma dose minutos depois,
    // cada um achando que o outro nao registrou. Sem recusa, as duas passam e o historico conta
    // dose dobrada — e o dano e silencioso, porque ninguem e avisado.
    DOSE_ALREADY_REGISTERED(149, "This dose is already registered for this animal on this date"),
    // A equipe: ajustar funcao e desligar sao atos de administracao, e o alvo pode nao ser desta
    // organizacao — 404 nesse caso, pela mesma razao de sempre (403 confirmaria que aquele
    // vinculo existe).
    MEMBERSHIP_NOT_FOUND(150, "Membership not found"),
    ADMINISTRATOR_ROLE_REQUIRED(151, "Only an administrator of this organization can do that"),
    // A organizacao nao pode ficar sem quem a administre: sem administrador ninguem convida,
    // ajusta funcao nem desliga, e ela vira um cadastro que so o suporte destrava.
    LAST_ADMINISTRATOR(152, "This organization would be left without an administrator"),
    // Aceitar convite para uma organizacao de que a pessoa ja e membro ativo. Nao e erro do
    // usuario e nao e sucesso: recusar sem consumir o convite e o unico desfecho que nao mente.
    // Aceitar em silencio gastaria um convite de uso unico para nao mudar nada, e criar um
    // segundo vinculo daria a mesma pessoa duas funcoes na mesma organizacao — e a pergunta
    // "qual delas vale" nao tem resposta.
    ALREADY_ORGANIZATION_MEMBER(153, "You are already an active member of this organization"),
    // ------------------------------------------------ a uniao de cadastros duplicados (Tela 32)
    //
    // Um cadastro que ja foi absorvido nao recebe registro novo nem entra em pedido novo: ele e
    // um apontador para onde a vida do animal continua. Escrever nele criaria um evento que a
    // linha do tempo do animal nunca mostraria — perdido num cadastro que ninguem mais le.
    ANIMAL_ALREADY_MERGED(154, "This record was merged into another one"),
    MERGE_REQUEST_NOT_FOUND(155, "Merge request not found"),
    // Decidir duas vezes nao e idempotencia: a segunda decisao chegaria de alguem que leu a
    // comparacao ANTES da primeira uniao acontecer, e estaria decidindo sobre um estado que ja
    // nao existe.
    MERGE_REQUEST_ALREADY_DECIDED(156, "This merge request was already decided"),
    // O mesmo par, pedido duas vezes. Quem responde receberia a mesma pergunta em duplicata e
    // aceitaria as duas — e a segunda tentaria absorver um cadastro que ja foi absorvido.
    MERGE_REQUEST_ALREADY_PENDING(157, "There is already a pending merge request for these two records"),
    // ---------------------------------------------------- o combinado da creche (Tela 41)
    //
    // CODIGO PROPRIO, e nao INVALID_REQUEST: um dia da semana escrito errado — "SEGUNDA", "MON",
    // "1" — nao e um formulario mal preenchido pelo tutor, e um cliente falando outra lingua. A
    // tela precisa poder dizer isso ao desenvolvedor em vez de mandar a creche "conferir os
    // campos marcados", que aqui nao ajudaria ninguem.
    INVALID_WEEKDAY(158, "Weekday must be one of MONDAY..SUNDAY"),
    INVALID_REQUEST(400, "Invalid request"),
    INVALID_CREDENTIALS(401, "Invalid email or password"),
    // estava escrito a mao dentro do handler generico, fora deste enum - ou seja, uma
    // segunda fonte para a mesma pergunta. O cliente traduz POR CODIGO, entao um codigo
    // que nao aparece aqui e um codigo que a tabela do front nao tem como cobrir
    INTERNAL_ERROR(500, "Internal server error");

    private final int code;
    private final String message;

    ErrorMessageEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

}
