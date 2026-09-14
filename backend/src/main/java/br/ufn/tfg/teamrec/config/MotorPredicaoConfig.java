package br.ufn.tfg.teamrec.config;

import br.ufn.tfg.teamrec.integration.prediction.MotorPredicaoClient;
import br.ufn.tfg.teamrec.integration.prediction.MotorPredicaoProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Monta o cliente do motor de predicao com endereco e timeouts externalizados.
 *
 * Os timeouts sao o que impede uma lentidao do servico Python de segurar
 * as threads do backend indefinidamente.
 */
@Configuration
@EnableConfigurationProperties(MotorPredicaoProperties.class)
public class MotorPredicaoConfig {

    @Bean
    public MotorPredicaoClient motorPredicaoClient(RestClient.Builder builder, MotorPredicaoProperties propriedades) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(propriedades.connectTimeout());
        fabrica.setReadTimeout(propriedades.readTimeout());

        return new MotorPredicaoClient(builder
                .baseUrl(propriedades.baseUrl())
                .requestFactory(fabrica)
                .build());
    }
}
