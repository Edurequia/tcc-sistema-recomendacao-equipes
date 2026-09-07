package br.ufn.tfg.teamrec.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Habilita o preenchimento automatico de {@code criadoEm} e {@code atualizadoEm}
 * declarados em {@link br.ufn.tfg.teamrec.domain.entity.EntidadeBase}.
 *
 * O provedor de data e hora precisa ser explicito: o padrao do Spring Data
 * produz {@code LocalDateTime} e nao consegue converte-lo para
 * {@code OffsetDateTime}. As entidades usam OffsetDateTime de proposito, para
 * que os carimbos trafeguem na API com o fuso preservado.
 */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "provedorDataHoraAuditoria")
public class JpaAuditingConfig {

    @Bean
    public DateTimeProvider provedorDataHoraAuditoria() {
        return () -> Optional.of(OffsetDateTime.now());
    }
}
