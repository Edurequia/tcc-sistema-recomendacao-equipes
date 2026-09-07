package br.ufn.tfg.teamrec.controller;

import br.ufn.tfg.teamrec.domain.enums.CriticidadeProjeto;
import br.ufn.tfg.teamrec.domain.enums.StatusProjeto;
import br.ufn.tfg.teamrec.dto.request.ProjetoRequest;
import br.ufn.tfg.teamrec.dto.request.StatusProjetoRequest;
import br.ufn.tfg.teamrec.dto.response.PaginaResponse;
import br.ufn.tfg.teamrec.dto.response.ProjetoResponse;
import br.ufn.tfg.teamrec.dto.response.ProjetoResumoResponse;
import br.ufn.tfg.teamrec.service.ProjetoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Endpoints do cadastro de projetos e de seus requisitos.
 *
 * Este e o lado da demanda: e aqui que o gestor descreve contra o que os
 * desenvolvedores serao avaliados na recomendacao.
 */
@RestController
@RequestMapping("/api/projetos")
@RequiredArgsConstructor
@Tag(name = "Projetos", description = "Projetos e os requisitos para formacao das equipes.")
public class ProjetoController {

    private final ProjetoService service;

    @GetMapping
    @Operation(summary = "Lista projetos de forma paginada",
            description = "Todos os filtros sao opcionais e podem ser combinados livremente.")
    public PaginaResponse<ProjetoResumoResponse> listar(
            @Parameter(description = "Busca parcial por nome, sem diferenciar maiusculas.")
            @RequestParam(required = false) String nome,

            @Parameter(description = "Filtra pelo ciclo de vida do projeto.")
            @RequestParam(required = false) StatusProjeto status,

            @Parameter(description = "Filtra pelo nivel de criticidade.")
            @RequestParam(required = false) CriticidadeProjeto criticidade,

            @PageableDefault(size = 20, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {

        return service.listar(nome, status, criticidade, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um projeto com todos os seus requisitos",
            description = "Inclui os papeis exigidos e a stack tecnologica requerida.")
    public ProjetoResponse buscar(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    @Operation(summary = "Cadastra um projeto",
            description = "O somatorio das vagas por papel nao pode exceder o tamanho da equipe.")
    public ResponseEntity<ProjetoResponse> criar(@Valid @RequestBody ProjetoRequest request,
                                                 UriComponentsBuilder uriBuilder) {
        ProjetoResponse criado = service.criar(request);
        return ResponseEntity
                .created(uriBuilder.path("/api/projetos/{id}").build(criado.id()))
                .body(criado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza o cadastro completo",
            description = "Papeis e tecnologias enviados SUBSTITUEM os anteriores por inteiro.")
    public ProjetoResponse atualizar(@PathVariable Long id,
                                     @Valid @RequestBody ProjetoRequest request) {
        return service.atualizar(id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Altera apenas o status do projeto",
            description = "Projetos em PLANEJAMENTO ou EM_ANDAMENTO consomem horas dos "
                    + "desenvolvedores; concluir ou cancelar libera essa alocacao.")
    public ProjetoResponse alterarStatus(@PathVariable Long id,
                                         @Valid @RequestBody StatusProjetoRequest request) {
        return service.alterarStatus(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cancela um projeto",
            description = "Equivalente a exclusao logica: o projeto passa a CANCELADO, para de "
                    + "disputar desenvolvedores, mas continua existindo junto das recomendacoes "
                    + "ja geradas para ele.")
    public void cancelar(@PathVariable Long id) {
        service.cancelar(id);
    }
}
