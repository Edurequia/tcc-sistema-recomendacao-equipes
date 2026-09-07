package br.ufn.tfg.teamrec.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadados da documentacao OpenAPI, exposta em /swagger-ui.html.
 * Serve como contrato vivo entre este backend e o frontend React.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI teamrecOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("TeamRec API")
                .version("v1")
                .description("""
                        Backend orquestrador do Sistema Web de Recomendacao para Formacao \
                        de Equipes em Projetos de Desenvolvimento de Software.

                        Trabalho Final de Graduacao - Curso de Sistemas de Informacao - \
                        UFN (Universidade Franciscana).""")
                .contact(new Contact()
                        .name("Eduardo Requia Souza")
                        .email("requia.eduardo@ufn.edu.br"))
                .license(new License().name("Uso academico")));
    }
}
