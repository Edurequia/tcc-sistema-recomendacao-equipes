package br.ufn.tfg.teamrec.integration.llm;

import br.ufn.tfg.teamrec.domain.enums.CriticidadeProjeto;
import br.ufn.tfg.teamrec.domain.enums.PapelAtuacao;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Requisitos de projeto reconhecidos no texto livre do gestor.
 *
 * Todos os campos sao opcionais na desserializacao: o modelo preenche o que o
 * texto informa e deixa o resto nulo. Cabe a {@link #faltantes()} dizer o que
 * impede seguir adiante, em vez de completar com valores inventados.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ParametrosExtraidos(
        String nome,
        String descricao,
        CriticidadeProjeto criticidade,
        Integer tamanhoEquipe,
        Integer horasSemanaisPorIntegrante,
        List<PapelExtraido> papeis,
        List<TecnologiaExtraida> tecnologias) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PapelExtraido(PapelAtuacao papel, Integer quantidade) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TecnologiaExtraida(String nome, Integer peso, Boolean obrigatoria) {

        public TecnologiaExtraida {
            peso = peso == null ? 3 : peso;
            obrigatoria = obrigatoria == null || obrigatoria;
        }
    }

    public ParametrosExtraidos {
        nome = nome == null || nome.isBlank() ? null : nome.trim();
        descricao = descricao == null || descricao.isBlank() ? null : descricao.trim();
    }

    /**
     * Criticidade media quando o texto nao a menciona: e o nivel intermediario
     * da escala, o unico palpite que nao distorce a ponderacao para nenhum lado.
     *
     * O padrao e aplicado aqui, e nao na desserializacao, para que
     * {@link #criticidade()} continue devolvendo nulo quando o gestor nada
     * disse. O que o sistema mostra como "entendido" precisa distinguir o que
     * foi lido no texto do que foi assumido pelo sistema.
     */
    public CriticidadeProjeto criticidadeOuPadrao() {
        return criticidade == null ? CriticidadeProjeto.MEDIA : criticidade;
    }

    /** O que o texto do gestor precisa dizer e nao disse. */
    public List<String> faltantes() {
        List<String> faltantes = new ArrayList<>();
        if (nome == null) {
            faltantes.add("o nome do projeto");
        }
        if (tamanhoEquipe == null || tamanhoEquipe < 1) {
            faltantes.add("o tamanho da equipe");
        }
        if (horasSemanaisPorIntegrante == null || horasSemanaisPorIntegrante < 1) {
            faltantes.add("as horas semanais por integrante");
        }
        return faltantes;
    }

    public List<PapelExtraido> papeisOuVazio() {
        return papeis == null ? List.of() : papeis.stream().filter(p -> p.papel() != null).toList();
    }

    public List<TecnologiaExtraida> tecnologiasOuVazio() {
        return tecnologias == null ? List.of()
                : tecnologias.stream().filter(t -> t.nome() != null && !t.nome().isBlank()).toList();
    }
}
