package br.ufn.tfg.teamrec.mapper;

import br.ufn.tfg.teamrec.domain.entity.Recomendacao;
import br.ufn.tfg.teamrec.domain.entity.RecomendacaoMembro;
import br.ufn.tfg.teamrec.dto.response.MembroRankingResponse;
import br.ufn.tfg.teamrec.dto.response.MetricasEquipeResponse;
import br.ufn.tfg.teamrec.dto.response.RecomendacaoResponse;
import br.ufn.tfg.teamrec.dto.response.RecomendacaoResumoResponse;
import br.ufn.tfg.teamrec.service.composicao.MetricasAgregadas;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

@Mapper
public interface RecomendacaoMapper {

    @Mapping(target = "projetoId", source = "projeto.id")
    @Mapping(target = "projetoNome", source = "projeto.nome")
    @Mapping(target = "origemDescricao", source = "origem.descricao")
    @Mapping(target = "tamanhoEquipe", source = "projeto.tamanhoEquipe")
    @Mapping(target = "totalCandidatos", expression = "java(recomendacao.getMembros().size())")
    @Mapping(target = "integrantesSelecionados", expression = "java(recomendacao.getMembrosSelecionados().size())")
    @Mapping(target = "metricas", source = ".")
    @Mapping(target = "ranking", source = "membros")
    @Mapping(target = "avisoJustificativa", ignore = true)
    RecomendacaoResponse toResponse(Recomendacao recomendacao);

    MetricasEquipeResponse toMetricas(Recomendacao recomendacao);

    MetricasEquipeResponse fromAgregadas(MetricasAgregadas metricas);

    @Mapping(target = "projetoId", source = "projeto.id")
    @Mapping(target = "origemDescricao", source = "origem.descricao")
    RecomendacaoResumoResponse toResumo(Recomendacao recomendacao);

    @Mapping(target = "posicao", source = "posicaoRanking")
    @Mapping(target = "desenvolvedorId", source = "desenvolvedor.id")
    @Mapping(target = "nome", source = "desenvolvedor.nome")
    @Mapping(target = "senioridade", source = "desenvolvedor.senioridade")
    @Mapping(target = "senioridadeDescricao", source = "desenvolvedor.senioridade.descricao")
    @Mapping(target = "papel", source = "papelSugerido")
    @Mapping(target = "papelDescricao", source = "papelSugerido.descricao")
    @Mapping(target = "elegivel", expression = "java(elegivel(membro))")
    MembroRankingResponse toMembro(RecomendacaoMembro membro);

    /** Mesma regra da composicao: elegivel e quem cobre todas as horas exigidas. */
    default boolean elegivel(RecomendacaoMembro membro) {
        return membro.getCoberturaHoras() != null && membro.getCoberturaHoras().compareTo(BigDecimal.ONE) >= 0;
    }
}
