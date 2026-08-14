package br.com.petfy.healthcare.notification;

import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.Enrollment;
import br.com.petfy.healthcare.domain.entity.Grant;
import br.com.petfy.healthcare.domain.entity.Organization;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.repository.EnrollmentRepository;
import br.com.petfy.healthcare.domain.repository.GrantRepository;
import br.com.petfy.healthcare.service.AnimalReach;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Avisa quem cuidava do animal que ele morreu (Tela 33).
 *
 * <b>E o unico jeito de a tela poder dizer o que o desenho escreveu:</b> "a Clinica Vet Norte e a
 * Creche Quintal sao avisadas, sem que voce precise ligar para cada uma". A alternativa nao era
 * um aviso pior — era o tutor telefonando para tres lugares no dia em que perdeu o animal, para
 * cada um deles perguntar como ele esta.
 *
 * <b>Duas listas de destinatario, e nao uma.</b> O {@link AnimalReach} resolve pessoas — a
 * co-tutora, a veterinaria com acesso pessoal — e nao serve para organizacao, porque a pergunta
 * que ele responde e "quem poderia responder por este animal", e uma clinica nunca responderia.
 * A creche entra por um caminho proprio: matricula viva, que nao e concessao nenhuma.
 *
 * <b>Falhar aqui nao pode desfazer o encerramento</b>, pela mesma razao ja escrita no
 * {@link OrganizationActivityNotifier}: o efeito principal e o registro, e perde-lo porque o SMTP
 * caiu trocaria um problema pequeno por um grande. Pior ainda neste caso — o tutor teria de
 * preencher o formulario de novo.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnimalDeathNotifier {

    private static final String EVENTO = "obito do animal";

    private final AsyncNotificationDispatcher dispatcher;
    private final AnimalReach animalReach;
    private final GrantRepository grantRepository;
    private final EnrollmentRepository enrollmentRepository;

    /**
     * @param quemEncerrou quem preencheu o formulario, excluido dos avisos: ele acabou de fazer
     *                     isso e nao precisa que o sistema o informe do que ele mesmo escreveu.
     */
    public void animalMorreu(Animal animal, Person quemEncerrou) {
        avisarPessoas(animal, quemEncerrou);
        avisarOrganizacoes(animal);
    }

    private void avisarPessoas(Animal animal, Person quemEncerrou) {
        for (Person destinatario : animalReach.pessoas(animal.getAnimalId())) {
            if (quemEncerrou != null
                    && destinatario.getPersonId().equals(quemEncerrou.getPersonId())) {
                continue;
            }

            /*
             * O e-mail nao sai para endereco nao confirmado, e o AVISO sai assim mesmo — o in-app so
             * aparece para quem ja entrou na conta. Ate a V48 isto era um `continue`: quem nao tinha
             * confirmado o e-mail nao ficava sabendo, por canal nenhum, que o animal tinha morrido.
             */
            boolean porEmail = destinatario.podeReceberNotificacao();

            if (!porEmail) {
                log.info("Pessoa {} ainda nao confirmou o e-mail; aviso de {} fica so no app",
                        destinatario.getPersonId(), EVENTO);
            }

            despachar(() -> Notification.builder()
                    .toEmail(destinatario.getEmail())
                    .toName(destinatario.getName())
                    .subject("O " + animal.getName() + " morreu")
                    .lines(paraPessoa(animal))
                    .porEmail(porEmail)
                    .build());
        }
    }

    /**
     * A clinica com acesso vivo e a creche com matricula viva, sem repetir quem e as duas coisas.
     *
     * <b>O mapa por id existe porque a creche costuma ser tambem quem tem concessao</b> — ela
     * precisa da carteira para deixar o animal entrar —, e a mesma organizacao apareceria nas duas
     * consultas. Dois e-mails identicos sobre a morte do mesmo animal e o tipo de descuido que faz
     * uma pessoa desconfiar de que o sistema esta confuso.
     */
    private void avisarOrganizacoes(Animal animal) {
        Map<UUID, Organization> destinos = new LinkedHashMap<>();

        grantRepository
                .findVigentesDeOrganizacoesNoAnimal(animal.getAnimalId(), LocalDateTime.now())
                .stream()
                .map(Grant::getGranteeOrganization)
                .forEach(organizacao -> destinos.put(organizacao.getOrganizationId(), organizacao));

        enrollmentRepository.findVivasDoAnimal(animal.getAnimalId()).stream()
                .map(Enrollment::getClassGroup)
                .filter(java.util.Objects::nonNull)
                .map(turma -> turma.getOrganization())
                .filter(java.util.Objects::nonNull)
                .forEach(organizacao -> destinos.put(organizacao.getOrganizationId(), organizacao));

        for (Organization organizacao : destinos.values()) {
            // organizacao sem e-mail cadastrado nao e erro: o campo e opcional desde a Tela 15, e
            // um abrigo pode ter sido cadastrado so com telefone
            if (organizacao.getEmail() == null || organizacao.getEmail().isBlank()) {
                log.info("Organizacao {} nao tem e-mail; aviso de {} suprimido",
                        organizacao.getOrganizationId(), EVENTO);
                continue;
            }

            despachar(() -> Notification.builder()
                    .toEmail(organizacao.getEmail())
                    .toName(organizacao.getName())
                    .subject("O " + animal.getName() + " morreu")
                    .lines(paraOrganizacao(animal))
                    .build());
        }
    }

    /**
     * O texto para quem convivia com o animal.
     *
     * <b>Nao manda condolencias e nao sugere nada.</b> O desenho e explicito sobre o que o produto
     * nao faz aqui, e a caixa de e-mail e onde essa regra e mais facil de quebrar: um "sentimos
     * muito pela sua perda" automatico assinado por um sistema e exatamente a frase que ninguem
     * quer receber de um sistema.
     */
    private List<String> paraPessoa(Animal animal) {
        List<String> linhas = new ArrayList<>();
        linhas.add("Quem responde pelo " + animal.getName() + " encerrou a linha do tempo dele no Petfy.");
        linhas.add("");
        linhas.add("Os avisos sobre ele param hoje. O que foi registrado continua no Petfy, "
                + "e voce pode abrir quando quiser.");
        return linhas;
    }

    /**
     * O texto para a clinica e para a creche, que precisam de uma informacao a mais: o que muda na
     * operacao delas. A creche tinha o animal esperado na segunda-feira.
     */
    private List<String> paraOrganizacao(Animal animal) {
        List<String> linhas = new ArrayList<>();
        linhas.add("Quem responde pelo " + animal.getName() + " encerrou a linha do tempo dele no Petfy.");
        linhas.add("");
        linhas.add("A matricula dele, se havia uma, foi encerrada, e nenhuma pendencia dele "
                + "continua sendo cobrada.");
        linhas.add("O que sua organizacao registrou na vida dele continua no Petfy.");
        return linhas;
    }

    /** O try e por destinatario: falhar para um nao pode calar os outros. */
    private void despachar(java.util.function.Supplier<Notification> mensagem) {
        try {
            dispatcher.dispatch(mensagem.get(), EVENTO);
        } catch (Exception e) {
            log.error("Falha ao preparar o aviso de {}", EVENTO, e);
        }
    }

}
