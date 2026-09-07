package br.ufn.tfg.teamrec.controller;

import br.ufn.tfg.teamrec.domain.enums.PapelAtuacao;
import br.ufn.tfg.teamrec.domain.enums.Senioridade;
import br.ufn.tfg.teamrec.dto.request.DesenvolvedorRequest;
import br.ufn.tfg.teamrec.dto.request.DisponibilidadeRequest;
import br.ufn.tfg.teamrec.dto.response.DesenvolvedorResponse;
import br.ufn.tfg.teamrec.dto.response.DesenvolvedorResumoResponse;
import br.ufn.tfg.teamrec.dto.response.PaginaResponse;
import br.ufn.tfg.teamrec.service.DesenvolvedorService;
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
 * Endpoints do cadastro de desenvolvedores.
 *
 * A camada e fina de proposito: recebe, valida via {@code @Valid} e delega ao
 * service. Nao ha consulta ao banco nem tratamento de erro aqui.
 */
@RestController
@RequestMapping("/api/desenvolvedores")
@RequiredArgsConstructor
@Tag(name = "Desenvolvedores", description = "Perfis dos profissionais candidatos a compor equipes.")
public class DesenvolvedorController {

    private final DesenvolvedorService service;

    @GetMapping
    @Operation(summary = "Lista desenvolvedores de forma paginada",
            description = "Todos os filtros sao opcionais e podem ser combinados livremente.")
    public PaginaResponse<DesenvolvedorResumoResponse> listar(
            @Parameter(description = "Busca parcial por nome, sem diferenciar maiusculas.")
            @RequestParam(required = false) String nome,

            @Parameter(description = "Filtra por nivel de experiencia.")
            @RequestParam(required = false) Senioridade senioridade,

            @Parameter(description = "Filtra por area tecnica de atuacao.")
            @RequestParam(required = false) PapelAtuacao papel,

            @Parameter(description = "Filtra por situacao. Omitido, traz ativos e inativos.")
            @RequestParam(required = false) Boolean ativo,

            @PageableDefault(size = 20, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {

        return service.listar(nome, senioridade, papel, ativo, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca o perfil completo de um desenvolvedor",
            description = "Inclui a lista de competencias com o nivel de proficiencia.")
    public DesenvolvedorResponse buscar(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    @Operation(summary = "Cadastra um desenvolvedor")
    public ResponseEntity<DesenvolvedorResponse> criar(@Valid @RequestBody DesenvolvedorRequest request,
                                                       UriComponentsBuilder uriBuilder) {
        DesenvolvedorResponse criado = service.criar(request);
        return ResponseEntity
                .created(uriBuilder.path("/api/desenvolvedores/{id}").build(criado.id()))
                .body(criado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza o perfil completo",
            description = "A lista de competencias enviada SUBSTITUI a anterior por inteiro.")
    public DesenvolvedorResponse atualizar(@PathVariable Long id,
                                           @Valid @RequestBody DesenvolvedorRequest request) {
        return service.atualizar(id, request);
    }

    @PatchMapping("/{id}/disponibilidade")
    @Operation(summary = "Ajusta apenas a carga horaria disponivel",
            description = "Operacao pontual para o proprio desenvolvedor, sem risco de "
                    + "sobrescrever as competencias. Recusada se as horas informadas forem "
                    + "menores que as ja comprometidas em projetos ativos.")
    public DesenvolvedorResponse atualizarDisponibilidade(@PathVariable Long id,
                                                          @Valid @RequestBody DisponibilidadeRequest request) {
        return service.atualizarDisponibilidade(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Inativa um desenvolvedor",
            description = "Inativacao logica: ele deixa de ser candidato em novas recomendacoes, "
                    + "mas permanece visivel nas que ja o incluem.")
    public void inativar(@PathVariable Long id) {
        service.inativar(id);
    }

    @PostMapping("/{id}/reativacao")
    @Operation(summary = "Reativa um desenvolvedor inativado")
    public DesenvolvedorResponse reativar(@PathVariable Long id) {
        return service.reativar(id);
    }
}
