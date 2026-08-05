package br.com.petfy.healthcare.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Limita o tamanho maximo de pagina a 100 registros para evitar que
 * ?size=999999 empurre o problema de volume para o banco.
 *
 * Defaults: page=0, size=20, sem sort obrigatorio.
 */
@Configuration
public class PageableConfig implements WebMvcConfigurer {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        PageableHandlerMethodArgumentResolver resolver = new PageableHandlerMethodArgumentResolver();
        resolver.setDefaultPageable(PageRequest.of(0, DEFAULT_PAGE_SIZE));
        resolver.setMaxPageSize(MAX_PAGE_SIZE);
        resolvers.add(resolver);
    }
}
