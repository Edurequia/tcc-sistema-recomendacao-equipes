package br.ufn.tfg.teamrec.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Envelope de paginacao proprio da API.
 *
 * O {@code Page} do Spring Data nao e serializado de forma estavel: sua
 * estrutura JSON ja mudou entre versoes e o proprio framework desaconselha
 * expo-lo diretamente. Este record fixa o contrato que o frontend consome.
 */
public record PaginaResponse<T>(
        List<T> conteudo,
        int pagina,
        int tamanho,
        long totalElementos,
        int totalPaginas,
        boolean primeira,
        boolean ultima
) {

    /** Converte uma pagina de entidades em uma pagina de DTOs. */
    public static <E, D> PaginaResponse<D> de(Page<E> pagina, Function<E, D> conversor) {
        return new PaginaResponse<>(
                pagina.getContent().stream().map(conversor).toList(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages(),
                pagina.isFirst(),
                pagina.isLast());
    }
}
