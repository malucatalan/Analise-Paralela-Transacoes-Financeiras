import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class EstatisticasCategoria {

    // Estrutura de dados enxuta para evitar estouro de memória RAM
    class ResumoCategoria {
        int quantidade = 0;
        double valorTotal = 0.0;
        double maiorTransacao = 0.0;
        double menorTransacao = Double.MAX_VALUE;
    }

    // Estrutura para consolidar a resposta global com o tempo medido
    public class ResultadoFinalCategorias {
        public Map<String, ResumoCategoria> mapa = new HashMap<>();
        public long tempoExecucaoMs = 0;
    }

    // -------------------------------------------------------------
    // EXECUÇÃO SEQUENCIAL (1 THREAD)
    // -------------------------------------------------------------
    public ResultadoFinalCategorias calcularSequencial(List<Transacao> transacoes) {
        ResultadoFinalCategorias resultadoGlobal = new ResultadoFinalCategorias();
        resultadoGlobal.mapa = inicializarMapa();

        long inicio = System.currentTimeMillis();

        // O computador lê a lista inteira do início ao fim
        for (Transacao t : transacoes) {
            processarTransacao(resultadoGlobal.mapa, t);
        }

        long fim = System.currentTimeMillis();
        resultadoGlobal.tempoExecucaoMs = fim - inicio;

        System.out.println("\n[RESULTADO SEQUENCIAL - 1 THREAD]");
        imprimirTabela(resultadoGlobal.mapa, resultadoGlobal.tempoExecucaoMs);

        return resultadoGlobal;
    }

    // -------------------------------------------------------------
    // EXECUÇÃO PARALELA (EXECUTOR SERVICE)
    // -------------------------------------------------------------
    public ResultadoFinalCategorias calcularParalelo(List<Transacao> transacoes, int numThreads) {
        ResultadoFinalCategorias resultadoGlobal = new ResultadoFinalCategorias();

        long inicio = System.currentTimeMillis();

        // EXECUTOR: cria o pool fixo de threads
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        // FUTURE: Lista de promessas dos resultados parciais
        List<Future<Map<String, ResumoCategoria>>> resultados = new ArrayList<>();

        // Calcula o tamanho da fatia da lista por thread
        int tamanhoFatia = transacoes.size() / numThreads;

        // Criação e distribuição das tarefas
        for (int i = 0; i < numThreads; i++) {
            int inicioFatia = i * tamanhoFatia;
            int fimFatia = (i == numThreads - 1) ? transacoes.size() : (i + 1) * tamanhoFatia;

            // Separa a fatia exata dos dados e cria a tarefa para a thread resolver
            Callable<Map<String, ResumoCategoria>> tarefa = new TarefaEstatisticas(transacoes.subList(inicioFatia, fimFatia));

            // Submete ao executor e guarda a promessa
            resultados.add(executor.submit(tarefa));
        }

        // Mapa principal onde junta as respostas de todas as threads no final (Redução)
        resultadoGlobal.mapa = inicializarMapa();

        try {
            // Loop que junta as respostas parciais
            for (Future<Map<String, ResumoCategoria>> resultado : resultados) {

                // .get() aguarda a conclusão da thread e pega o mapa parcial dela
                Map<String, ResumoCategoria> mapaParcial = resultado.get();

                // Combina os valores da thread no mapa principal
                for (String categoria : resultadoGlobal.mapa.keySet()) {
                    ResumoCategoria fimResumo = resultadoGlobal.mapa.get(categoria);
                    ResumoCategoria parcResumo = mapaParcial.get(categoria);

                    fimResumo.quantidade += parcResumo.quantidade;
                    fimResumo.valorTotal += parcResumo.valorTotal;

                    if (parcResumo.maiorTransacao > fimResumo.maiorTransacao) {
                        fimResumo.maiorTransacao = parcResumo.maiorTransacao;
                    }
                    if (parcResumo.menorTransacao < fimResumo.menorTransacao) {
                        fimResumo.menorTransacao = parcResumo.menorTransacao;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Desliga o executor
        executor.shutdown();

        long fim = System.currentTimeMillis();
        resultadoGlobal.tempoExecucaoMs = fim - inicio;

        System.out.println("\n[RESULTADO PARALELO - " + numThreads + " THREADS]");
        imprimirTabela(resultadoGlobal.mapa, resultadoGlobal.tempoExecucaoMs);

        return resultadoGlobal;
    }

    // -------------------------------------------------------------
    // TAREFA PARALELA (CALLABLE)
    // -------------------------------------------------------------
    class TarefaEstatisticas implements Callable<Map<String, ResumoCategoria>> {
        private final List<Transacao> fatia;

        public TarefaEstatisticas(List<Transacao> fatia) {
            this.fatia = fatia;
        }

        @Override
        public Map<String, ResumoCategoria> call() {
            // Instância local e isolada para evitar race condition entre threads
            Map<String, ResumoCategoria> mapaLocal = inicializarMapa();

            // Processa apenas a fatia designada para esta thread
            for (Transacao t : fatia) {
                processarTransacao(mapaLocal, t);
            }

            return mapaLocal;
        }
    }

    // Método auxiliar para criar as categorias zeradas
    private Map<String, ResumoCategoria> inicializarMapa() {
        Map<String, ResumoCategoria> mapa = new HashMap<>();
        mapa.put("Alimentação", new ResumoCategoria());
        mapa.put("Transporte", new ResumoCategoria());
        mapa.put("Eletrônicos", new ResumoCategoria());
        mapa.put("Saúde", new ResumoCategoria());
        mapa.put("Educação", new ResumoCategoria());
        return mapa;
    }

    // Processamento de cada elemento do vetor
    private void processarTransacao(Map<String, ResumoCategoria> mapa, Transacao t) {
        ResumoCategoria resumo = mapa.get(t.categoria);
        if (resumo != null) {
            resumo.quantidade++;
            resumo.valorTotal += t.valor;
            if (t.valor > resumo.maiorTransacao) resumo.maiorTransacao = t.valor;
            if (t.valor < resumo.menorTransacao) resumo.menorTransacao = t.valor;
        }
    }

    // Impressão em formato de tabela
    private void imprimirTabela(Map<String, ResumoCategoria> mapaEstatisticas, long tempoExecucaoMs) {
        System.out.printf("\n%-16s %12s %20s %18s %18s %18s\n", 
                          "Categoria", "Quantidade", "Total", "Média", "Menor Trans.", "Maior Trans.");
        System.out.println("---------------------------------------------------------------------------------------------------------------");

        for (String nomeCategoria : mapaEstatisticas.keySet()) {
            ResumoCategoria res = mapaEstatisticas.get(nomeCategoria);

            if (res.quantidade > 0) {
                double media = res.valorTotal / res.quantidade;

                System.out.printf("%-16s %,12d    R$ %,14.2f   R$ %,13.2f   R$ %,13.2f   R$ %,13.2f\n", 
                                  nomeCategoria, res.quantidade, res.valorTotal, media, res.menorTransacao, res.maiorTransacao);
            }
        }
        System.out.println("---------------------------------------------------------------------------------------------------------------");
        System.out.printf("Tempo de Execução: %,d ms (%.3f segundos)\n", tempoExecucaoMs, tempoExecucaoMs / 1000.0);
        System.out.println("---------------------------------------------------------------------------------------------------------------\n");
    }
}