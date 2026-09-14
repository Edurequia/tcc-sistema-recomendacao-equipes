package br.ufn.tfg.teamrec.controller;

import br.ufn.tfg.teamrec.dto.response.AlocacaoResponse;
import br.ufn.tfg.teamrec.dto.response.LinhaBaseResponse;
import br.ufn.tfg.teamrec.dto.response.RecomendacaoResponse;
import br.ufn.tfg.teamrec.dto.response.RecomendacaoResumoResponse;
import br.ufn.tfg.teamrec.service.AlocacaoService;
import br.ufn.tfg.teamrec.service.RecomendacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    @PostMapping("/projetos/{projetoId}/recomendacoes")
    @Operation(summary = "Gera uma recomendacao para o projeto",
            description = "Pontua todos os desenvolvedores ativos no motor de predicao e grava o "
                    + "ranking completo. Responde 503 se o motor estiver indisponivel.")
    public ResponseEntity<RecomendacaoResponse> gerar(@PathVariable Long projetoId,
                                                      UriComponentsBuilder uriBuilder) {
        RecomendacaoResponse gerada = service.gerar(projetoId);
        return ResponseEntity
                .created(uriBuilder.path("/api/recomendacoes/{id}").build(gerada.id()))
                .body(gerada);
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

    @PostMapping("/recomendacoes/{id}/confirmacao")
    @Operation(summary = "Confirma a equipe proposta, alocando os selecionados no projeto",
            description = "Tudo ou nada: se algum selecionado nao tiver mais horas livres, nenhuma "
                    + "alocacao e criada (422) e a recomendacao deve ser gerada de novo.")
    public List<AlocacaoResponse> confirmar(@PathVariable Long id) {
        return alocacaoService.confirmarRecomendacao(id);
    }
}
