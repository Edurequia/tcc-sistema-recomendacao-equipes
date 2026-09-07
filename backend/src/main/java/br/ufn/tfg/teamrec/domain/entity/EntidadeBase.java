package br.ufn.tfg.teamrec.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;

/**
 * Superclasse de todas as entidades: identificador e carimbos de auditoria.
 *
 * Centralizar isso evita repetir os mesmos quatro campos em nove entidades e
 * garante que {@code equals}/{@code hashCode} sigam a unica implementacao
 * correta para JPA:
 *
 * <ul>
 *   <li>{@code equals} compara apenas o id, e retorna false enquanto a entidade
 *       for transiente (id nulo) — duas instancias ainda nao persistidas nunca
 *       sao consideradas iguais;</li>
 *   <li>{@code hashCode} e constante por tipo. Se dependesse do id, o hash
 *       mudaria no momento do flush e a entidade se perderia dentro de um
 *       {@code HashSet}.</li>
 * </ul>
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class EntidadeBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @LastModifiedDate
    @Column(nullable = false)
    private OffsetDateTime atualizadoEm;

    @Override
    public final boolean equals(Object objeto) {
        if (this == objeto) {
            return true;
        }
        if (!(objeto instanceof EntidadeBase outra)) {
            return false;
        }
        // Compara o tipo efetivo para nao igualar entidades distintas de mesmo id.
        if (!getClass().equals(outra.getClass())) {
            return false;
        }
        return id != null && id.equals(outra.id);
    }

    @Override
    public final int hashCode() {
        return getClass().hashCode();
    }
}
