package br.ufn.tfg.teamrec.mapper;

import br.ufn.tfg.teamrec.domain.entity.Habilidade;
import br.ufn.tfg.teamrec.dto.response.HabilidadeResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Converte {@link Habilidade} nos DTOs de saida.
 *
 * O MapStruct gera a implementacao em tempo de compilacao: nao ha reflexao em
 * execucao, e um campo do DTO que deixe de existir na entidade quebra o build
 * em vez de virar {@code null} silenciosamente.
 */
@Mapper
public interface HabilidadeMapper {

    @Mapping(target = "tipoDescricao", source = "tipo.descricao")
    @Mapping(target = "categoriaDescricao", source = "categoria.descricao")
    @Mapping(target = "usadaNoModelo", expression = "java(habilidade.possuiColunaNoModelo())")
    HabilidadeResponse toResponse(Habilidade habilidade);

    List<HabilidadeResponse> toResponseList(List<Habilidade> habilidades);
}
