package br.ufn.tfg.teamrec.controller;

import br.ufn.tfg.teamrec.dto.request.AlocacaoRequest;
import br.ufn.tfg.teamrec.dto.response.AlocacaoResponse;
import br.ufn.tfg.teamrec.service.AlocacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/** Endpoints das alocacoes confirmadas de desenvolvedores em projetos. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Alocacoes", description = "Compromissos confirmados, que consomem as horas dos desenvolvedores.")
public class AlocacaoController {

    private final AlocacaoService service;

    @PostMapping("/projetos/{projetoId}/alocacoes")
    @Operation(summary = "Aloca um desenvolvedor no projeto",
            description = "Recusada com 422 se o desenvolvedor nao tiver horas livres suficientes, "
                    + "somadas as alocacoes em outros projetos ativos.")
    public ResponseEntity<AlocacaoResponse> alocar(@PathVariable Long projetoId,
                                                   @Valid @RequestBody AlocacaoRequest request,
                                                   UriComponentsBuilder uriBuilder) {
        AlocacaoResponse criada = service.alocar(projetoId, request);
        return ResponseEntity
                .created(uriBuilder.path("/api/alocacoes/{id}").build(criada.id()))
                .body(criada);
    }

    @GetMapping("/projetos/{projetoId}/alocacoes")
    @Operation(summary = "Lista a equipe alocada no projeto (alocacoes ativas)")
    public List<AlocacaoResponse> listar(@PathVariable Long projetoId) {
        return service.listarAtivasDoProjeto(projetoId);
    }

    @GetMapping("/alocacoes/{id}")
    @Operation(summary = "Busca uma alocacao, ativa ou encerrada")
    public AlocacaoResponse buscar(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @DeleteMapping("/alocacoes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Encerra uma alocacao",
            description = "Encerramento logico: libera as horas do desenvolvedor e preserva o historico.")
    public void encerrar(@PathVariable Long id) {
        service.encerrar(id);
    }
}
