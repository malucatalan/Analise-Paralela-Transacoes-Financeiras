import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import net.openhft.affinity.AffinityLock;

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
    // 1. EXECUÇÃO SEQUENCIAL (1 THREAD)
    // -------------------------------------------------------------
    public ResultadoFinalCategorias calcularSequencial(List<Transacao> transacoes) {
        ResultadoFinalCategorias resultadoGlobal = new ResultadoFinalCategorias();
        resultadoGlobal.mapa = inicializarMapa();

        long inicio = System.currentTimeMillis();

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
    // 2. EXECUÇÃO PARALELA (EXECUTOR SERVICE)
    // -------------------------------------------------------------
    public ResultadoFinalCategorias calcularParalelo(List<Transacao> transacoes, int numThreads) {
        ResultadoFinalCategorias resultadoGlobal = new ResultadoFinalCategorias();
        long inicio = System.currentTimeMillis();

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<Map<String, ResumoCategoria>>> resultados = new ArrayList<>();
        int tamanhoFatia = transacoes.size() / numThreads;

        for (int i = 0; i < numThreads; i++) {
            int inicioFatia = i * tamanhoFatia;
            int fimFatia = (i == numThreads - 1) ? transacoes.size() : (i + 1) * tamanhoFatia;

            Callable<Map<String, ResumoCategoria>> tarefa = new TarefaEstatisticas(transacoes.subList(inicioFatia, fimFatia));
            resultados.add(executor.submit(tarefa));
        }

        resultadoGlobal.mapa = consolidarMapas(resultados);

        executor.shutdown();

        long fim = System.currentTimeMillis();
        resultadoGlobal.tempoExecucaoMs = fim - inicio;

        System.out.println("\n[RESULTADO PARALELO - " + numThreads + " THREADS]");
        imprimirTabela(resultadoGlobal.mapa, resultadoGlobal.tempoExecucaoMs);

        return resultadoGlobal;
    }

    // -------------------------------------------------------------
    // 3. EXECUÇÃO PARALELA COM AFINIDADE DE CPU (OPENHFT AFFINITY)
    // -------------------------------------------------------------
    public ResultadoFinalCategorias calcularParaleloComAffinity(List<Transacao> transacoes, int numThreads) {
        ResultadoFinalCategorias resultadoGlobal = new ResultadoFinalCategorias();
        long inicio = System.currentTimeMillis();

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<Map<String, ResumoCategoria>>> resultados = new ArrayList<>();
        int tamanhoFatia = transacoes.size() / numThreads;
        int totalCores = Runtime.getRuntime().availableProcessors();

        for (int i = 0; i < numThreads; i++) {
            int inicioFatia = i * tamanhoFatia;
            int fimFatia = (i == numThreads - 1) ? transacoes.size() : (i + 1) * tamanhoFatia;
            
            // Proteção para não estourar o limite de núcleos físicos
            int cpuCore = i % totalCores;
            
            Callable<Map<String, ResumoCategoria>> tarefa = new TarefaEstatisticasAffinity(transacoes.subList(inicioFatia, fimFatia), cpuCore);
            resultados.add(executor.submit(tarefa));
        }

        resultadoGlobal.mapa = consolidarMapas(resultados);

        executor.shutdown();

        long fim = System.currentTimeMillis();
        resultadoGlobal.tempoExecucaoMs = fim - inicio;

        System.out.println("\n[RESULTADO PARALELO + AFINIDADE - " + numThreads + " THREADS FIXADAS EM CORES]");
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
            Map<String, ResumoCategoria> mapaLocal = inicializarMapa();
            for (Transacao t : fatia) {
                processarTransacao(mapaLocal, t);
            }
            return mapaLocal;
        }
    }

    // -------------------------------------------------------------
    // TAREFA PARALELA COM AFINIDADE (CALLABLE)
    // -------------------------------------------------------------
    class TarefaEstatisticasAffinity implements Callable<Map<String, ResumoCategoria>> {
        private final List<Transacao> fatia;
        private final int cpuCore;

        public TarefaEstatisticasAffinity(List<Transacao> fatia, int cpuCore) {
            this.fatia = fatia;
            this.cpuCore = cpuCore;
        }

        @Override
        public Map<String, ResumoCategoria> call() {
            try (AffinityLock lock = AffinityLock.acquireLock(cpuCore)) {
                Map<String, ResumoCategoria> mapaLocal = inicializarMapa();
                for (Transacao t : fatia) {
                    processarTransacao(mapaLocal, t);
                }
                return mapaLocal;
            }
        }
    }

    // -------------------------------------------------------------
    // MÉTODOS AUXILIARES
    // -------------------------------------------------------------
    private Map<String, ResumoCategoria> inicializarMapa() {
        Map<String, ResumoCategoria> mapa = new HashMap<>();
        mapa.put("Alimentação", new ResumoCategoria());
        mapa.put("Transporte", new ResumoCategoria());
        mapa.put("Eletrônicos", new ResumoCategoria());
        mapa.put("Saúde", new ResumoCategoria());
        mapa.put("Educação", new ResumoCategoria());
        return mapa;
    }

    private void processarTransacao(Map<String, ResumoCategoria> mapa, Transacao t) {
        ResumoCategoria resumo = mapa.get(t.categoria);
        if (resumo != null) {
            resumo.quantidade++;
            resumo.valorTotal += t.valor;
            if (t.valor > resumo.maiorTransacao) resumo.maiorTransacao = t.valor;
            if (t.valor < resumo.menorTransacao) resumo.menorTransacao = t.valor;
        }
    }

    private Map<String, ResumoCategoria> consolidarMapas(List<Future<Map<String, ResumoCategoria>>> resultados) {
        Map<String, ResumoCategoria> mapaFinal = inicializarMapa();
        try {
            for (Future<Map<String, ResumoCategoria>> resultado : resultados) {
                Map<String, ResumoCategoria> mapaParcial = resultado.get();
                for (String categoria : mapaFinal.keySet()) {
                    ResumoCategoria fimResumo = mapaFinal.get(categoria);
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
        return mapaFinal;
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