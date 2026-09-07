package br.ufn.tfg.teamrec.domain.repository;

import br.ufn.tfg.teamrec.domain.entity.Projeto;
import br.ufn.tfg.teamrec.domain.enums.CriticidadeProjeto;
import br.ufn.tfg.teamrec.domain.enums.StatusProjeto;
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
public interface ProjetoRepository extends JpaRepository<Projeto, Long> {

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);

    List<Projeto> findByStatusOrderByNomeAsc(StatusProjeto status);

    /** Projetos que disputam desenvolvedores; base do calculo de conflito de alocacao. */
    List<Projeto> findByStatusInOrderByNomeAsc(Collection<StatusProjeto> status);

    /**
     * Listagem paginada com filtros opcionais.
     * Cada filtro nulo se anula na propria clausula.
     */
    @Query("""
            select p from Projeto p
            where (:nome is null or lower(p.nome) like lower(concat('%', :nome, '%')))
              and (:status is null or p.status = :status)
              and (:criticidade is null or p.criticidade = :criticidade)
            """)
    Page<Projeto> buscarComFiltros(@Param("nome") String nome,
                                   @Param("status") StatusProjeto status,
                                   @Param("criticidade") CriticidadeProjeto criticidade,
                                   Pageable pageable);

    /**
     * As duas consultas abaixo carregam colecoes distintas do mesmo projeto.
     *
     * Elas foram mantidas separadas de proposito: um unico JOIN FETCH sobre as
     * duas colecoes produziria um produto cartesiano entre papeis e habilidades.
     * Chamadas dentro da mesma transacao, ambas retornam a MESMA instancia
     * gerenciada, que ao final fica com as duas colecoes inicializadas.
     */
    @Query("""
            select distinct p from Projeto p
            left join fetch p.papeisRequeridos
            where p.id = :id
            """)
    Optional<Projeto> buscarComPapeisRequeridos(@Param("id") Long id);

    @Query("""
            select distinct p from Projeto p
            left join fetch p.habilidadesRequeridas hr
            left join fetch hr.habilidade
            where p.id = :id
            """)
    Optional<Projeto> buscarComHabilidadesRequeridas(@Param("id") Long id);
}
