package br.ufn.tfg.teamrec.domain.repository;

import br.ufn.tfg.teamrec.domain.entity.Desenvolvedor;
import br.ufn.tfg.teamrec.domain.enums.PapelAtuacao;
import br.ufn.tfg.teamrec.domain.enums.Senioridade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DesenvolvedorRepository extends JpaRepository<Desenvolvedor, Long> {

    Optional<Desenvolvedor> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    /** Usado na atualizacao: verifica duplicidade de e-mail ignorando o proprio registro. */
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    List<Desenvolvedor> findByAtivoTrueOrderByNomeAsc();

    /**
     * Carrega um desenvolvedor com suas habilidades em uma unica consulta.
     * Sem o JOIN FETCH, cada habilidade dispararia um SELECT adicional (N+1).
     */
    @Query("""
            select distinct d from Desenvolvedor d
            left join fetch d.habilidades dh
            left join fetch dh.habilidade
            where d.id = :id
            """)
    Optional<Desenvolvedor> buscarComHabilidades(@Param("id") Long id);

    /**
     * Listagem paginada com filtros opcionais.
     *
     * Cada filtro nulo se anula na propria clausula, o que permite combinar
     * qualquer subconjunto deles sem montar a consulta dinamicamente.
     */
    @Query("""
            select d from Desenvolvedor d
            where (:nome is null or lower(d.nome) like lower(concat('%', :nome, '%')))
              and (:senioridade is null or d.senioridade = :senioridade)
              and (:papel is null or d.papelPrincipal = :papel)
              and (:ativo is null or d.ativo = :ativo)
            """)
    Page<Desenvolvedor> buscarComFiltros(@Param("nome") String nome,
                                         @Param("senioridade") Senioridade senioridade,
                                         @Param("papel") PapelAtuacao papel,
                                         @Param("ativo") Boolean ativo,
                                         Pageable pageable);

    /**
     * Carrega todos os candidatos ativos com suas habilidades ja resolvidas.
     *
     * E a consulta que alimenta a montagem do vetor de atributos: o motor de
     * predicao avalia todos os candidatos de uma vez, entao carregar as
     * habilidades em lote aqui evita centenas de consultas na Fase 4.
     */
    @Query("""
            select distinct d from Desenvolvedor d
            left join fetch d.habilidades dh
            left join fetch dh.habilidade
            where d.ativo = true
            order by d.nome asc
            """)
    List<Desenvolvedor> buscarAtivosComHabilidades();

    /** Varios perfis com as competencias em uma consulta, ativos ou nao. */
    @Query("""
            select distinct d from Desenvolvedor d
            left join fetch d.habilidades dh
            left join fetch dh.habilidade
            where d.id in :ids
            """)
    List<Desenvolvedor> buscarComHabilidadesPorIds(@Param("ids") Collection<Long> ids);
}
