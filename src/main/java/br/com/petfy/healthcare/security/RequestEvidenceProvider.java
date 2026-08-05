package br.com.petfy.healthcare.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * IP e user agent da requisicao em andamento, para servir de evidencia de aceite.
 *
 * Le do {@code RequestContextHolder} em vez de o controller passar o
 * {@code HttpServletRequest} adiante - mesmo padrao do {@link CurrentOwnerProvider},
 * que resolve o titular pelo {@code SecurityContextHolder}. Assim o controller
 * continua sem saber que existe evidencia a coletar, e o dia em que outro fluxo
 * precisar dela nao pede mudanca de assinatura em cadeia.
 *
 * <b>Devolve nulo fora de requisicao</b>, e nao lanca: a rotina de lembrete roda em
 * scheduler, e um aceite gravado por caminho sem HTTP nao pode falhar por nao ter de
 * onde tirar IP. Evidencia ausente nao invalida consentimento.
 */
@Component
public class RequestEvidenceProvider {

    private static final int MAX_USER_AGENT = 512;

    /**
     * IP de origem, respeitando {@code X-Forwarded-For} quando presente.
     *
     * O provedor de hospedagem termina o TLS e encaminha, entao
     * {@code getRemoteAddr()} sozinho devolveria o IP do proxy - a mesma evidencia
     * para todo mundo, o que e o mesmo que nao ter evidencia. O primeiro valor do
     * cabecalho e o cliente original; os seguintes sao os proxies do caminho.
     *
     * <b>O cabecalho e informado pelo cliente e pode ser forjado.</b> Isto e
     * evidencia, nao autenticacao - nada de seguranca depende dele, e para o proposito
     * (mostrar de onde partiu o aceite) o valor encaminhado e o mais util que existe.
     */
    public String ip() {
        HttpServletRequest request = request();

        if (request == null) {
            return null;
        }

        String encaminhado = request.getHeader("X-Forwarded-For");

        if (encaminhado != null && !encaminhado.isBlank()) {
            return truncar(encaminhado.split(",")[0].trim(), 45);
        }

        return truncar(request.getRemoteAddr(), 45);
    }

    public String userAgent() {
        HttpServletRequest request = request();

        return request != null ? truncar(request.getHeader("User-Agent"), MAX_USER_AGENT) : null;
    }

    /**
     * Trunca em vez de deixar o banco recusar: user agent nao tem limite pratico de
     * tamanho, e uma coluna estourada faria a criacao de conta falhar por causa de um
     * navegador verborragico.
     */
    private String truncar(String valor, int limite) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        return valor.length() <= limite ? valor : valor.substring(0, limite);
    }

    private HttpServletRequest request() {
        RequestAttributes atributos = RequestContextHolder.getRequestAttributes();

        return atributos instanceof ServletRequestAttributes servlet ? servlet.getRequest() : null;
    }

}
