//DEPS net.openhft:affinity:3.23.3

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import net.openhft.affinity.AffinityLock;

public class TransacoesSuspeitas {

    public class ResumoSuspeitas {
        public int totalAnalisado = 0;
        public int quantidadeSuspeitas = 0;
        public double valorTotalSuspeito = 0.0;
        public List<Transacao> listaSuspeitas = new ArrayList<>();
        public long tempoExecucaoMs = 0;
    }

    public ResumoSuspeitas calcularSequencial(List<Transacao> transacoes) {
        ResumoSuspeitas resumo = new ResumoSuspeitas();

        long inicio = System.currentTimeMillis();

        for (Transacao t : transacoes) {
            processarTransacao(resumo, t);
        }

        long fim = System.currentTimeMillis();
        resumo.tempoExecucaoMs = fim - inicio;

        return resumo;
    }

    public ResumoSuspeitas calcularParalelo(List<Transacao> transacoes, int numThreads) {
        long inicio = System.currentTimeMillis();

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<ResumoSuspeitas>> resultados = new ArrayList<>();
        int tamanhoFatia = transacoes.size() / numThreads;

        for (int i = 0; i < numThreads; i++) {
            int inicioFatia = i * tamanhoFatia;
            int fimFatia = (i == numThreads - 1) ? transacoes.size() : (i + 1) * tamanhoFatia;

            Callable<ResumoSuspeitas> tarefa = new TarefaSuspeitas(transacoes.subList(inicioFatia, fimFatia));
            resultados.add(executor.submit(tarefa));
        }

        ResumoSuspeitas resumoFinal = consolidarResultados(resultados);

        executor.shutdown();

        long fim = System.currentTimeMillis();
        resumoFinal.tempoExecucaoMs = fim - inicio;

        return resumoFinal;
    }

    public ResumoSuspeitas calcularParaleloComAffinity(List<Transacao> transacoes, int numThreads) {
        long inicio = System.currentTimeMillis();

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<ResumoSuspeitas>> resultados = new ArrayList<>();
        int tamanhoFatia = transacoes.size() / numThreads;

        for (int i = 0; i < numThreads; i++) {
            int inicioFatia = i * tamanhoFatia;
            int fimFatia = (i == numThreads - 1) ? transacoes.size() : (i + 1) * tamanhoFatia;

            int cpuCore = i;
            Callable<ResumoSuspeitas> tarefa = new TarefaSuspeitasAffinity(transacoes.subList(inicioFatia, fimFatia),
                    cpuCore);
            resultados.add(executor.submit(tarefa));
        }

        ResumoSuspeitas resumoFinal = consolidarResultados(resultados);

        executor.shutdown();

        long fim = System.currentTimeMillis();
        resumoFinal.tempoExecucaoMs = fim - inicio;

        return resumoFinal;
    }

    class TarefaSuspeitas implements Callable<ResumoSuspeitas> {
        private final List<Transacao> fatia;

        public TarefaSuspeitas(List<Transacao> fatia) {
            this.fatia = fatia;
        }

        @Override
        public ResumoSuspeitas call() {
            ResumoSuspeitas resumoLocal = new ResumoSuspeitas();
            for (Transacao t : fatia) {
                processarTransacao(resumoLocal, t);
            }
            return resumoLocal;
        }
    }

    class TarefaSuspeitasAffinity implements Callable<ResumoSuspeitas> {
        private final List<Transacao> fatia;
        private final int cpuCore;

        public TarefaSuspeitasAffinity(List<Transacao> fatia, int cpuCore) {
            this.fatia = fatia;
            this.cpuCore = cpuCore;
        }

        @Override
        public ResumoSuspeitas call() {
            try (AffinityLock lock = AffinityLock.acquireLock(cpuCore)) {
                ResumoSuspeitas resumoLocal = new ResumoSuspeitas();
                for (Transacao t : fatia) {
                    processarTransacao(resumoLocal, t);
                }
                return resumoLocal;
            }
        }
    }

    private ResumoSuspeitas consolidarResultados(List<Future<ResumoSuspeitas>> resultados) {
        ResumoSuspeitas resumoFinal = new ResumoSuspeitas();
        try {
            for (Future<ResumoSuspeitas> resultado : resultados) {
                ResumoSuspeitas parcial = resultado.get();
                resumoFinal.totalAnalisado += parcial.totalAnalisado;
                resumoFinal.quantidadeSuspeitas += parcial.quantidadeSuspeitas;
                resumoFinal.valorTotalSuspeito += parcial.valorTotalSuspeito;
                resumoFinal.listaSuspeitas.addAll(parcial.listaSuspeitas);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resumoFinal;
    }

    private void processarTransacao(ResumoSuspeitas resumo, Transacao t) {
        resumo.totalAnalisado++;

        boolean criterio1 = t.valor > 10000.0;
        boolean criterio2 = t.valor > 5000.0 && "Eletrônicos".equalsIgnoreCase(t.categoria);

        if (criterio1 || criterio2) {
            resumo.quantidadeSuspeitas++;
            resumo.valorTotalSuspeito += t.valor;
            resumo.listaSuspeitas.add(t);
        }
    }

    public void imprimirRelatorio(ResumoSuspeitas resumo) {
        double percentualSuspeito = (resumo.totalAnalisado > 0)
                ? ((double) resumo.quantidadeSuspeitas / resumo.totalAnalisado) * 100
                : 0.0;

        System.out.println("------------------------------------------------------------------");
        System.out.println("DETECÇÃO DE TRANSAÇÕES SUSPEITAS");
        System.out.println("------------------------------------------------------------------");
        System.out.printf("Total de transações analisadas: %,d\n", resumo.totalAnalisado);
        System.out.printf("Transações suspeitas encontradas: %,d (%.2f%%)\n", resumo.quantidadeSuspeitas,
                percentualSuspeito);
        System.out.printf("Valor acumulado em suspeitas: R$ %,14.2f\n", resumo.valorTotalSuspeito);
        System.out.printf("Tempo de Execução: %,d ms (%.3f segundos)\n", resumo.tempoExecucaoMs,
                resumo.tempoExecucaoMs / 1000.0);
        System.out.println("------------------------------------------------------------------\n");
    }
}