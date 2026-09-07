package br.ufn.tfg.teamrec.controller;

import br.ufn.tfg.teamrec.domain.enums.TipoHabilidade;
import br.ufn.tfg.teamrec.dto.request.HabilidadeRequest;
import br.ufn.tfg.teamrec.dto.response.HabilidadeResponse;
import br.ufn.tfg.teamrec.service.HabilidadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/**
 * Endpoints do catalogo de habilidades.
 *
 * A camada e deliberadamente fina: recebe, valida via {@code @Valid} e delega.
 * Nenhum tratamento de erro aqui — isso e responsabilidade do
 * {@code GlobalExceptionHandler}.
 */
@RestController
@RequestMapping("/api/habilidades")
@RequiredArgsConstructor
@Tag(name = "Habilidades", description = "Catalogo de competencias tecnicas e comportamentais.")
public class HabilidadeController {

    private final HabilidadeService service;

    @GetMapping
    @Operation(summary = "Lista o catalogo de habilidades")
    public List<HabilidadeResponse> listar(
            @Parameter(description = "Filtra por tipo de competencia.")
            @RequestParam(required = false) TipoHabilidade tipo,

            @Parameter(description = "Quando falso, inclui tambem as habilidades inativas.")
            @RequestParam(defaultValue = "true") boolean apenasAtivas) {

        return service.listar(tipo, apenasAtivas);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma habilidade pelo identificador")
    public HabilidadeResponse buscar(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    @Operation(summary = "Cadastra uma habilidade no catalogo")
    public ResponseEntity<HabilidadeResponse> criar(@Valid @RequestBody HabilidadeRequest request,
                                                    UriComponentsBuilder uriBuilder) {
        HabilidadeResponse criada = service.criar(request);
        return ResponseEntity
                .created(uriBuilder.path("/api/habilidades/{id}").build(criada.id()))
                .body(criada);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza uma habilidade")
    public HabilidadeResponse atualizar(@PathVariable Long id,
                                        @Valid @RequestBody HabilidadeRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Inativa uma habilidade",
            description = "Inativacao logica: a habilidade sai das listagens, mas os vinculos "
                    + "e recomendacoes que ja a referenciam permanecem intactos.")
    public void inativar(@PathVariable Long id) {
        service.inativar(id);
    }

    @PostMapping("/{id}/reativacao")
    @Operation(summary = "Reativa uma habilidade inativada")
    public HabilidadeResponse reativar(@PathVariable Long id) {
        return service.reativar(id);
    }
}
