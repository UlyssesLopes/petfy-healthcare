package br.com.petfy.healthcare;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Falha se os testes de container nao vao rodar.
 *
 * <b>Existe porque a rede de seguranca deste projeto ja foi desligada em silencio duas
 * vezes.</b> As classes que estendem {@link PostgresContainerTest} usam
 * {@code disabledWithoutDocker = true}, o que as faz serem <i>puladas</i> - e nao
 * falharem - quando o Testcontainers conclui que nao ha Docker. A conclusao dele nao e
 * confiavel: {@code isDockerAvailable()} engole qualquer {@code Throwable} e devolve
 * {@code false}, entao pressao de memoria, negociacao de versao de API ou um daemon
 * ocupado viram "sem Docker".
 *
 * O resultado nos dois casos foi o mesmo: <b>build verde com zero migration validada
 * contra Postgres</b>, exatamente a falsa confianca que custou quatro bugs de producao
 * nesta base - FK e indice unico que so o banco recusa.
 *
 * <ul>
 *   <li>Primeira vez: o docker-java assumia API 1.32 no named pipe do Windows e o
 *       daemon recusava. Mitigado com {@code api.version} no surefire.</li>
 *   <li>Segunda vez: <b>causa nao identificada.</b> Na suite completa toda classe de
 *       container era pulada; isoladas, passavam. Fixar o heap no surefire fez o
 *       sintoma desaparecer de forma reproduzivel, mas isso e evidencia e nao
 *       explicacao - ver o comentario no pom.</li>
 * </ul>
 *
 * Duas mitigacoes pontuais para o mesmo sintoma sao sinal de que faltava o guarda. Este
 * teste e o guarda: em vez de descobrir pelo numero de "Skipped" que ninguem le, o build
 * para.
 *
 * <b>Maquina sem Docker:</b> rode com
 * {@code -Dpetfy.allow-skipping-container-tests=true}. E uma escolha explicita, que
 * aparece no comando - o oposto de um skip silencioso.
 */
@DisplayName("os testes de container precisam poder rodar")
class ContainerTestsHabilitadosTest {

    @Test
    @DisplayName("Docker tem de estar disponivel, senao a suite roda sem validar o banco")
    void dockerTemDeEstarDisponivel() {
        assumeFalse(Boolean.getBoolean("petfy.allow-skipping-container-tests"),
                "pulo autorizado explicitamente por -Dpetfy.allow-skipping-container-tests=true");

        assertThat(DockerClientFactory.instance().isDockerAvailable())
                .as("O Testcontainers nao encontrou Docker, entao TODA classe *ContainerTest "
                        + "sera pulada e o build passaria sem validar migration, chave estrangeira "
                        + "nem indice unico contra Postgres. Se a maquina realmente nao tem Docker, "
                        + "rode com -Dpetfy.allow-skipping-container-tests=true. Se tem, o motivo "
                        + "provavel e memoria: isDockerAvailable() devolve false em vez de propagar "
                        + "o erro.")
                .isTrue();
    }

}
