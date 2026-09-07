package br.ufn.tfg.teamrec.mapper;

import br.ufn.tfg.teamrec.domain.entity.Desenvolvedor;
import br.ufn.tfg.teamrec.domain.entity.DesenvolvedorHabilidade;
import br.ufn.tfg.teamrec.dto.response.DesenvolvedorResponse;
import br.ufn.tfg.teamrec.dto.response.DesenvolvedorResumoResponse;
import br.ufn.tfg.teamrec.dto.response.VinculoHabilidadeResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Converte {@link Desenvolvedor} nos DTOs de saida. */
@Mapper
public interface DesenvolvedorMapper {

    @Mapping(target = "senioridadeDescricao", source = "senioridade.descricao")
    @Mapping(target = "papelPrincipalDescricao", source = "papelPrincipal.descricao")
    DesenvolvedorResponse toResponse(Desenvolvedor desenvolvedor);

    @Mapping(target = "senioridadeDescricao", source = "senioridade.descricao")
    @Mapping(target = "papelPrincipalDescricao", source = "papelPrincipal.descricao")
    DesenvolvedorResumoResponse toResumo(Desenvolvedor desenvolvedor);

    @Mapping(target = "habilidadeId", source = "habilidade.id")
    @Mapping(target = "nome", source = "habilidade.nome")
    @Mapping(target = "tipo", source = "habilidade.tipo")
    @Mapping(target = "categoria", source = "habilidade.categoria")
    VinculoHabilidadeResponse toVinculoResponse(DesenvolvedorHabilidade vinculo);
}
