package br.ufn.tfg.teamrec.domain.repository;

import br.ufn.tfg.teamrec.domain.entity.Recomendacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecomendacaoRepository extends JpaRepository<Recomendacao, Long> {

    /** Historico completo de um projeto, da execucao mais recente para a mais antiga. */
    List<Recomendacao> findByProjetoIdOrderByCriadoEmDesc(Long projetoId);

    Optional<Recomendacao> findFirstByProjetoIdOrderByCriadoEmDesc(Long projetoId);

    /** Carrega a recomendacao com todo o ranking e os dados dos desenvolvedores. */
    @Query("""
            select distinct r from Recomendacao r
            left join fetch r.membros m
            left join fetch m.desenvolvedor
            join fetch r.projeto
            where r.id = :id
            order by m.posicaoRanking
            """)
    Optional<Recomendacao> buscarCompleta(@Param("id") Long id);

    /** Recomendacoes em que um desenvolvedor aparece; atende a visao do proprio profissional. */
    @Query("""
            select distinct r from Recomendacao r
            join r.membros m
            join fetch r.projeto
            where m.desenvolvedor.id = :desenvolvedorId and m.selecionado = true
            order by r.criadoEm desc
            """)
    List<Recomendacao> buscarPorDesenvolvedorSelecionado(@Param("desenvolvedorId") Long desenvolvedorId);
}
