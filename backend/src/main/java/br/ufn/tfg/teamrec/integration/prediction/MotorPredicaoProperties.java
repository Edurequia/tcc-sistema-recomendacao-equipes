package br.ufn.tfg.teamrec.integration.prediction;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuracao do motor de predicao, lida de {@code app.prediction.*}.
 *
 * @param baseUrl        endereco do servico FastAPI.
 * @param connectTimeout tempo maximo para abrir a conexao.
 * @param readTimeout    tempo maximo aguardando a resposta.
 */
@ConfigurationProperties(prefix = "app.prediction")
public record MotorPredicaoProperties(
        String baseUrl,
        Duration connectTimeout,
        Duration readTimeout
) {
}
