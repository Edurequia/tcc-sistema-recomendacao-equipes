package br.ufn.tfg.teamrec.config;

import br.ufn.tfg.teamrec.integration.llm.ExtratorParametros;
import br.ufn.tfg.teamrec.integration.llm.GeradorJustificativa;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Monta o gerador da justificativa.
 *
 * O bean existe mesmo sem chave de API configurada: o {@link ObjectProvider}
 * devolve null quando nao ha {@link ChatModel}, e a chamada falha com 503 em
 * vez de o endpoint desaparecer do contrato da API. Isso mantem o Swagger e o
 * frontend estaveis entre um ambiente com e outro sem a integracao ligada.
 *
 * O prompt de sistema fica em src/main/resources/prompts, versionado junto do
 * codigo: mudanca de prompt e mudanca de comportamento e precisa aparecer no
 * historico do repositorio.
 */
@Configuration
public class LlmConfig {

    @Bean
    public GeradorJustificativa geradorJustificativa(
            ObjectProvider<ChatModel> chatModel,
            @Value("classpath:prompts/justificativa-sistema.txt") Resource promptSistema) {
        return new GeradorJustificativa(chatModel::getIfAvailable, ler(promptSistema));
    }

    @Bean
    public ExtratorParametros extratorParametros(
            ObjectProvider<ChatModel> chatModel,
            @Value("classpath:prompts/extracao-parametros.txt") Resource promptSistema) {
        return new ExtratorParametros(chatModel::getIfAvailable, ler(promptSistema));
    }

    private static String ler(Resource recurso) {
        try {
            return recurso.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException("Nao foi possivel ler o prompt de sistema.", ex);
        }
    }
}
