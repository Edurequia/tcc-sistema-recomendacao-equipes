package br.ufn.tfg.teamrec.controller;

import br.ufn.tfg.teamrec.dto.request.ConsultaRequest;
import br.ufn.tfg.teamrec.dto.request.ConversaRequest;
import br.ufn.tfg.teamrec.dto.response.AlocacaoResponse;
import br.ufn.tfg.teamrec.dto.response.ConversaResponse;
import br.ufn.tfg.teamrec.dto.response.JustificativaResponse;
import br.ufn.tfg.teamrec.dto.response.LinhaBaseResponse;
import br.ufn.tfg.teamrec.dto.response.RecomendacaoResponse;
import br.ufn.tfg.teamrec.dto.response.RecomendacaoResumoResponse;
import br.ufn.tfg.teamrec.service.AlocacaoService;
import br.ufn.tfg.teamrec.service.ConversaService;
import br.ufn.tfg.teamrec.service.RecomendacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/**
 * Endpoints de geracao e consulta de recomendacoes.
 *
 * A recomendacao pertence ao projeto na criacao e na listagem, mas tem
 * identidade propria na consulta individual.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Recomendacoes", description = "Ranking de desenvolvedores calculado pelo motor de predicao.")
public class RecomendacaoController {

    private final RecomendacaoService service;
    private final AlocacaoService alocacaoService;
    private final ConversaService conversaService;

    @PostMapping("/projetos/{projetoId}/recomendacoes")
    @Operation(summary = "Gera uma recomendacao para o projeto",
            description = "Pontua todos os desenvolvedores ativos no motor de predicao, grava o "
                    + "ranking completo e devolve a recomendacao ja com a justificativa do modelo de "
                    + "linguagem. Se o modelo falhar, a recomendacao e devolvida sem o texto e com o "
                    + "motivo em avisoJustificativa. Responde 503 se o motor estiver indisponivel.")
    public ResponseEntity<RecomendacaoResponse> gerar(@PathVariable Long projetoId,
                                                      UriComponentsBuilder uriBuilder) {
        RecomendacaoResponse gerada = service.gerar(projetoId);
        return ResponseEntity
                .created(uriBuilder.path("/api/recomendacoes/{id}").build(gerada.id()))
                .body(gerada);
    }

    @PostMapping("/recomendacoes/conversa")
    @Operation(summary = "Gera uma recomendacao a partir de um pedido em linguagem natural",
            description = "O modelo de linguagem extrai os requisitos do texto, o projeto e criado "
                    + "com as mesmas validacoes do cadastro por formulario e a recomendacao segue "
                    + "a mesma pipeline, gravada com origem CHAT e a consulta original. Responde "
                    + "422 quando o texto nao informa o minimo necessario e 503 quando a "
                    + "integracao nao esta configurada ou o modelo esta indisponivel.")
    public ResponseEntity<RecomendacaoResponse> gerarPorConversa(@RequestBody @Valid ConsultaRequest request,
                                                                 UriComponentsBuilder uriBuilder) {
        RecomendacaoResponse gerada = service.gerarPorConversa(request.consulta());
        return ResponseEntity
                .created(uriBuilder.path("/api/recomendacoes/{id}").build(gerada.id()))
                .body(gerada);
    }

    @PostMapping("/recomendacoes/conversa/turnos")
    @Operation(summary = "Conduz a conversa ate o pedido virar uma recomendacao",
            description = "Recebe o historico completo da conversa e devolve, com HTTP 200, ou uma "
                    + "pergunta (status PERGUNTA) nomeando o que ainda falta, ou a recomendacao "
                    + "gerada (status RECOMENDACAO). O historico e mantido pelo cliente: o backend "
                    + "nao guarda estado de sessao.")
    public ConversaResponse conversar(@RequestBody @Valid ConversaRequest request) {
        return conversaService.responder(request);
    }

    @GetMapping("/projetos/{projetoId}/recomendacoes")
    @Operation(summary = "Historico de recomendacoes do projeto, da mais recente para a mais antiga")
    public List<RecomendacaoResumoResponse> listarPorProjeto(@PathVariable Long projetoId) {
        return service.listarPorProjeto(projetoId);
    }

    @GetMapping("/recomendacoes/{id}")
    @Operation(summary = "Busca uma recomendacao com o ranking completo")
    public RecomendacaoResponse buscar(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @GetMapping("/recomendacoes/{id}/linha-base")
    @Operation(summary = "Compara a equipe proposta com equipes sorteadas ao acaso",
            description = "Sorteia equipes do mesmo tamanho apenas entre os candidatos elegiveis desta "
                    + "recomendacao e devolve a media e o desvio padrao de cada metrica. A semente "
                    + "fixa torna o resultado reproduzivel.")
    public LinhaBaseResponse linhaBase(
            @PathVariable Long id,
            @Parameter(description = "Quantidade de equipes sorteadas.")
            @RequestParam(defaultValue = "100") @Min(1) @Max(10_000) int sorteios,
            @Parameter(description = "Semente do gerador aleatorio; a mesma semente repete o resultado.")
            @RequestParam(defaultValue = "42") long semente) {
        return service.linhaBase(id, sorteios, semente);
    }

    @PostMapping("/recomendacoes/{id}/justificativa")
    @Operation(summary = "Gera de novo, ou completa, a explicacao da equipe recomendada",
            description = "A geracao da recomendacao ja pede a justificativa; este endpoint serve "
                    + "para repetir quando o modelo falhou ou para pedir um novo texto. Envia ao "
                    + "modelo o ranking, as metricas e as restricoes ja gravados e guarda o texto "
                    + "na recomendacao. O texto ja existente e reaproveitado; use "
                    + "regerar=true para produzir um novo. Responde 503 quando a integracao nao "
                    + "esta configurada ou o modelo esta indisponivel.")
    public JustificativaResponse justificar(
            @PathVariable Long id,
            @Parameter(description = "Ignora a justificativa ja gravada e consulta o modelo de novo.")
            @RequestParam(defaultValue = "false") boolean regerar) {
        return service.justificar(id, regerar);
    }

    @PostMapping("/recomendacoes/{id}/confirmacao")
    @Operation(summary = "Confirma a equipe proposta, alocando os selecionados no projeto",
            description = "Tudo ou nada: se algum selecionado nao tiver mais horas livres, nenhuma "
                    + "alocacao e criada (422) e a recomendacao deve ser gerada de novo.")
    public List<AlocacaoResponse> confirmar(@PathVariable Long id) {
        return alocacaoService.confirmarRecomendacao(id);
    }
}
