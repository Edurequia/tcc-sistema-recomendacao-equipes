package br.ufn.tfg.teamrec.domain.repository;

import br.ufn.tfg.teamrec.domain.entity.Alocacao;
import br.ufn.tfg.teamrec.domain.enums.StatusProjeto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface AlocacaoRepository extends JpaRepository<Alocacao, Long> {

    List<Alocacao> findByDesenvolvedorIdAndAtivaTrue(Long desenvolvedorId);

    List<Alocacao> findByProjetoIdAndAtivaTrue(Long projetoId);

    boolean existsByDesenvolvedorIdAndProjetoIdAndAtivaTrue(Long desenvolvedorId, Long projetoId);

    /**
     * Total de horas semanais ja comprometidas por um desenvolvedor em projetos
     * que ainda consomem alocacao. Retorna zero quando nao ha nenhuma.
     */
    @Query("""
            select coalesce(sum(a.horasSemanais), 0) from Alocacao a
            where a.desenvolvedor.id = :desenvolvedorId
              and a.ativa = true
              and a.projeto.status in :statusAtivos
            """)
    int somarHorasComprometidas(@Param("desenvolvedorId") Long desenvolvedorId,
                                @Param("statusAtivos") Collection<StatusProjeto> statusAtivos);

    /**
     * Mesma soma da consulta acima, porem para varios desenvolvedores de uma vez.
     *
     * Desenvolvedores sem nenhuma alocacao ativa NAO aparecem no resultado —
     * quem consumir este metodo deve tratar a ausencia como zero horas.
     */
    @Query("""
            select a.desenvolvedor.id as desenvolvedorId, sum(a.horasSemanais) as totalHoras
            from Alocacao a
            where a.ativa = true
              and a.projeto.status in :statusAtivos
            group by a.desenvolvedor.id
            """)
    List<HorasAlocadasProjection> somarHorasComprometidasPorDesenvolvedor(
            @Param("statusAtivos") Collection<StatusProjeto> statusAtivos);
}
