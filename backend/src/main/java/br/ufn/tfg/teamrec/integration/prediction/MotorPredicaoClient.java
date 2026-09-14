package br.ufn.tfg.teamrec.integration.prediction;

import br.ufn.tfg.teamrec.shared.exception.ServicoExternoException;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Cliente HTTP do motor de predicao (FastAPI).
 *
 * Toda falha de comunicacao vira {@link ServicoExternoException}, traduzida em
 * 503 pelo handler global. Isso inclui a recusa por validacao (422): ela so
 * acontece se o backend montar um vetor fora do dominio de treinamento, o
 * que e defeito do orquestrador, nunca do usuario.
 */
public class MotorPredicaoClient {

    static final String SERVICO = "motor-predicao";

    private final RestClient restClient;

    public MotorPredicaoClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public PredicaoMotorResponse prever(PredicaoMotorRequest requisicao) {
        PredicaoMotorResponse resposta;
        try {
            resposta = restClient.post()
                    .uri("/predicoes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requisicao)
                    .retrieve()
                    .body(PredicaoMotorResponse.class);
        } catch (RestClientResponseException ex) {
            throw new ServicoExternoException(SERVICO,
                    "O motor de predicao recusou a requisicao (HTTP %d)."
                            .formatted(ex.getStatusCode().value()), ex);
        } catch (RestClientException ex) {
            throw new ServicoExternoException(SERVICO,
                    "O motor de predicao esta indisponivel no momento.", ex);
        }

        garantirEscoreParaTodos(requisicao, resposta);
        return resposta;
    }

    /** Um candidato sem escore sairia do ranking em silencio; melhor falhar. */
    private void garantirEscoreParaTodos(PredicaoMotorRequest requisicao, PredicaoMotorResponse resposta) {
        Set<Long> recebidos = resposta == null || resposta.predicoes() == null
                ? Set.of()
                : resposta.predicoes().stream().map(Predicao::id).collect(Collectors.toSet());

        List<Long> semEscore = requisicao.candidatos().stream()
                .map(CandidatoVetor::id)
                .filter(id -> !recebidos.contains(id))
                .toList();

        if (!semEscore.isEmpty()) {
            throw new ServicoExternoException(SERVICO,
                    "O motor de predicao nao devolveu escore para os candidatos %s.".formatted(semEscore),
                    null);
        }
    }
}
