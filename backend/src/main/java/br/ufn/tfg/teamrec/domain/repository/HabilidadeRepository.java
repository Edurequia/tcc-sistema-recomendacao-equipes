package br.ufn.tfg.teamrec.domain.repository;

import br.ufn.tfg.teamrec.domain.entity.Habilidade;
import br.ufn.tfg.teamrec.domain.enums.TipoHabilidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface HabilidadeRepository extends JpaRepository<Habilidade, Long> {

    Optional<Habilidade> findByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);

    List<Habilidade> findByAtivaTrueOrderByNomeAsc();

    List<Habilidade> findByTipoAndAtivaTrueOrderByNomeAsc(TipoHabilidade tipo);

    List<Habilidade> findByIdIn(Collection<Long> ids);

    /**
     * Habilidades que possuem coluna correspondente na matriz de treinamento.
     * Define exatamente quais posicoes do vetor de atributos serao preenchidas.
     */
    List<Habilidade> findByChaveDatasetIsNotNullOrderByChaveDatasetAsc();

    Optional<Habilidade> findByChaveDataset(String chaveDataset);
}
