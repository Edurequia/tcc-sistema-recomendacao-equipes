package br.ufn.tfg.teamrec.integration.llm;

import java.math.BigDecimal;
import java.util.List;

/**
 * Tudo que o modelo de linguagem precisa saber para justificar uma equipe,
 * ja em forma de texto simples e sem nenhuma dependencia de JPA.
 *
 * O ranking dos preteridos vai junto de proposito: sem o contraste, nao ha
 * como explicar por que um perfil foi escolhido no lugar de outro.
 */
public record ContextoJustificativa(
        String projeto,
        String criticidade,
        int tamanhoEquipe,
        int horasSemanaisPorIntegrante,
        List<String> papeisExigidos,
        List<String> tecnologiasExigidas,
        List<Integrante> equipe,
        List<Integrante> preteridos,
        Metricas metricas,
        String restricao) {

    public record Integrante(String nome, String papel, String senioridade, BigDecimal score,
                             BigDecimal aderenciaTecnica, BigDecimal coberturaHoras, boolean elegivel) {
    }

    public record Metricas(BigDecimal coberturaPapeis, BigDecimal coberturaTecnologias,
                           BigDecimal desvioPadraoSenioridade, BigDecimal scoreMedioEquipe) {
    }

    /**
     * Monta a mensagem de usuario enviada ao modelo.
     *
     * O formato e texto corrido rotulado, e nao JSON, porque o modelo escreve
     * melhor a partir de rotulos legiveis e porque assim o prompt enviado
     * continua auditavel a olho nu no log e nos testes.
     */
    public String formatar() {
        StringBuilder texto = new StringBuilder();

        texto.append("PROJETO\n")
                .append("Nome: ").append(projeto).append('\n')
                .append("Criticidade: ").append(criticidade).append('\n')
                .append("Equipe prevista: ").append(tamanhoEquipe).append(" integrantes, ")
                .append(horasSemanaisPorIntegrante).append(" horas semanais cada\n")
                .append("Papeis exigidos: ").append(lista(papeisExigidos)).append('\n')
                .append("Tecnologias exigidas: ").append(lista(tecnologiasExigidas)).append("\n\n");

        texto.append("EQUIPE SUGERIDA (").append(equipe.size()).append(" de ").append(tamanhoEquipe).append(")\n");
        equipe.forEach(integrante -> texto.append(linha(integrante)));

        if (!preteridos.isEmpty()) {
            texto.append("\nCANDIDATOS NAO SELECIONADOS\n");
            preteridos.forEach(integrante -> texto.append(linha(integrante)));
        }

        texto.append("\nMETRICAS DA EQUIPE\n")
                .append("Cobertura de papeis: ").append(metricas.coberturaPapeis()).append("%\n")
                .append("Cobertura de tecnologias: ").append(metricas.coberturaTecnologias()).append("%\n")
                .append("Desvio padrao da senioridade: ").append(metricas.desvioPadraoSenioridade())
                .append(" (menor indica equipe mais nivelada)\n")
                .append("Escore medio da equipe: ").append(metricas.scoreMedioEquipe()).append('\n');

        if (restricao != null && !restricao.isBlank()) {
            texto.append("\nRESTRICOES REGISTRADAS\n").append(restricao).append('\n');
        }

        return texto.toString();
    }

    private static String linha(Integrante integrante) {
        return "- %s | papel: %s | senioridade: %s | escore: %s | aderencia tecnica: %s | cobertura de horas: %s%s%n"
                .formatted(integrante.nome(), integrante.papel(), integrante.senioridade(), integrante.score(),
                        integrante.aderenciaTecnica(), integrante.coberturaHoras(),
                        integrante.elegivel() ? "" : " | INELEGIVEL: sem horas livres suficientes");
    }

    private static String lista(List<String> itens) {
        return itens == null || itens.isEmpty() ? "nenhum declarado" : String.join("; ", itens);
    }
}
