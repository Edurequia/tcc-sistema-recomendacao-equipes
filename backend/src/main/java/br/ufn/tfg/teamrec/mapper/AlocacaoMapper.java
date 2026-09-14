package br.ufn.tfg.teamrec.mapper;

import br.ufn.tfg.teamrec.domain.entity.Alocacao;
import br.ufn.tfg.teamrec.dto.response.AlocacaoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface AlocacaoMapper {

    @Mapping(target = "desenvolvedorId", source = "desenvolvedor.id")
    @Mapping(target = "desenvolvedorNome", source = "desenvolvedor.nome")
    @Mapping(target = "projetoId", source = "projeto.id")
    @Mapping(target = "projetoNome", source = "projeto.nome")
    @Mapping(target = "papelDescricao", source = "papel.descricao")
    AlocacaoResponse toResponse(Alocacao alocacao);
}
