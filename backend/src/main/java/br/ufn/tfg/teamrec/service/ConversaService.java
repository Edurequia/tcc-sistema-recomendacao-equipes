package br.ufn.tfg.teamrec.service;

import br.ufn.tfg.teamrec.dto.request.ConversaRequest;
import br.ufn.tfg.teamrec.dto.response.ConversaResponse;
import br.ufn.tfg.teamrec.dto.response.ParametrosEntendidosResponse;
import br.ufn.tfg.teamrec.dto.response.RecomendacaoResponse;
import br.ufn.tfg.teamrec.integration.llm.ExtratorParametros;
import br.ufn.tfg.teamrec.integration.llm.ParametrosExtraidos;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Via conversacional com historico: conduz o pedido do gestor ate ele ter o
 * necessario para virar uma recomendacao.
 *
 * A diferenca para a chamada avulsa e o que se faz com um pedido incompleto.
 * La ele e recusado com 422; aqui vira uma pergunta, e a conversa continua.
 */
@Service
public class ConversaService {

    private final ExtratorParametros extrator;
    private final RecomendacaoService recomendacaoService;

    public ConversaService(ExtratorParametros extrator, RecomendacaoService recomendacaoService) {
        this.extrator = extrator;
        this.recomendacaoService = recomendacaoService;
    }

    public ConversaResponse responder(ConversaRequest conversa) {
        ParametrosExtraidos parametros = extrator.extrairParcial(
                conversa.transcricao(), recomendacaoService.catalogoDeTecnologias());

        List<String> faltantes = parametros.faltantes();
        if (!faltantes.isEmpty()) {
            return new ConversaResponse(ConversaResponse.StatusConversa.PERGUNTA,
                    pergunta(faltantes), faltantes, entendido(parametros), null);
        }

        RecomendacaoResponse recomendacao =
                recomendacaoService.gerarPorParametros(parametros, conversa.falasDoGestor());

        return new ConversaResponse(ConversaResponse.StatusConversa.RECOMENDACAO,
                confirmacao(recomendacao), List.of(), entendido(parametros), recomendacao);
    }

    /**
     * A pergunta e montada pelo backend, e nao pedida ao modelo.
     *
     * Sao dois motivos: uma segunda chamada por turno dobraria o consumo da
     * cota da API, e a pergunta precisa nomear exatamente os campos que
     * faltam, coisa que uma resposta gerada pode contornar sem avisar.
     */
    private static String pergunta(List<String> faltantes) {
        return "Para montar a equipe, ainda preciso saber %s.".formatted(enumerar(faltantes));
    }

    private static String confirmacao(RecomendacaoResponse recomendacao) {
        String equipe = recomendacao.integrantesSelecionados() == recomendacao.tamanhoEquipe()
                ? "uma equipe de %d integrante(s)".formatted(recomendacao.integrantesSelecionados())
                : "uma equipe parcial, com %d dos %d integrantes previstos"
                        .formatted(recomendacao.integrantesSelecionados(), recomendacao.tamanhoEquipe());

        return "Criei o projeto \"%s\" e propus %s. Consulte o ranking completo ou peca a justificativa."
                .formatted(recomendacao.projetoNome(), equipe);
    }

    private static ParametrosEntendidosResponse entendido(ParametrosExtraidos parametros) {
        return new ParametrosEntendidosResponse(
                parametros.nome(),
                parametros.criticidade() == null ? null : parametros.criticidade().name(),
                parametros.tamanhoEquipe(),
                parametros.horasSemanaisPorIntegrante(),
                parametros.papeisOuVazio().stream()
                        .map(papel -> "%s (%d)".formatted(papel.papel().getDescricao(),
                                papel.quantidade() == null ? 1 : papel.quantidade()))
                        .toList(),
                parametros.tecnologiasOuVazio().stream()
                        .map(ParametrosExtraidos.TecnologiaExtraida::nome)
                        .toList());
    }

    /** "a, b e c" — a pergunta e lida por uma pessoa, nao por um parser. */
    private static String enumerar(List<String> itens) {
        if (itens.size() == 1) {
            return itens.get(0);
        }
        return String.join(", ", itens.subList(0, itens.size() - 1)) + " e " + itens.get(itens.size() - 1);
    }
}
