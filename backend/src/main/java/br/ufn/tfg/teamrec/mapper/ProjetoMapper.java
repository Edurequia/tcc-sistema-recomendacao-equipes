package br.ufn.tfg.teamrec.mapper;

import br.ufn.tfg.teamrec.domain.entity.Projeto;
import br.ufn.tfg.teamrec.domain.entity.ProjetoHabilidadeRequerida;
import br.ufn.tfg.teamrec.domain.entity.ProjetoPapelRequerido;
import br.ufn.tfg.teamrec.dto.response.HabilidadeRequeridaResponse;
import br.ufn.tfg.teamrec.dto.response.PapelRequeridoResponse;
import br.ufn.tfg.teamrec.dto.response.ProjetoResponse;
import br.ufn.tfg.teamrec.dto.response.ProjetoResumoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Converte {@link Projeto} nos DTOs de saida.
 *
 * O mapeamento completo so pode ser invocado com as colecoes de requisitos ja
 * inicializadas — o service garante isso antes de chamar.
 */
@Mapper
public interface ProjetoMapper {

    @Mapping(target = "criticidadeDescricao", source = "criticidade.descricao")
    @Mapping(target = "criticidadeNivel", source = "criticidade.nivel")
    @Mapping(target = "statusDescricao", source = "status.descricao")
    @Mapping(target = "vagasDeclaradas", expression = "java(projeto.totalDeVagasPorPapel())")
    @Mapping(target = "vagasSemPapelFixo",
            expression = "java(projeto.getTamanhoEquipe() - projeto.totalDeVagasPorPapel())")
    ProjetoResponse toResponse(Projeto projeto);

    @Mapping(target = "criticidadeDescricao", source = "criticidade.descricao")
    @Mapping(target = "statusDescricao", source = "status.descricao")
    ProjetoResumoResponse toResumo(Projeto projeto);

    @Mapping(target = "papelDescricao", source = "papel.descricao")
    PapelRequeridoResponse toPapelResponse(ProjetoPapelRequerido requisito);

    @Mapping(target = "habilidadeId", source = "habilidade.id")
    @Mapping(target = "nome", source = "habilidade.nome")
    @Mapping(target = "categoria", source = "habilidade.categoria")
    @Mapping(target = "usadaNoModelo", expression = "java(requisito.getHabilidade().possuiColunaNoModelo())")
    HabilidadeRequeridaResponse toHabilidadeResponse(ProjetoHabilidadeRequerida requisito);
}
