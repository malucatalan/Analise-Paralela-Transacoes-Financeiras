import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class AgregacaoFinanceira {
    public static class Resultado {
        public final long quantidade;
        public final double soma; 
        public final Transacao maior; 
        public final Transacao menor;

        private Resultado(long quantidade, double soma, Transacao maior, Transacao menor) {
            this.quantidade = quantidade;
            this.soma = soma;
            this.maior = maior;
            this.menor = menor;
        }
        public double media() {
            return quantidade == 0 ? 0.0 : soma / quantidade;
        }
    }
    public Resultado calcularSequencial(List<Transacao> transacoes){
        validarDados(transacoes);
        Acumulador acumulador = new Acumulador();
        for(Transacao transacao : transacoes) acumulador.adicionar(transacao);
        return acumulador.resultado();
    }
    public Resultado calcularParalelo(List<Transacao> transacoes, int numThreads){
        return calcularParalelo(transacoes, numThreads, false);
    }

    public Resultado calcularParaleloComAffinity(List<Transacao> transacoes, int numThreads){
        // Mantém a API de afinidade sem exigir a biblioteca nativa no caminho padrão.
        return calcularParalelo(transacoes, numThreads, false);
    }

    private Resultado calcularParalelo(List<Transacao> transacoes, int numThreads, boolean usarAffinity){
        validarDados(transacoes); 
        if (numThreads < 1) throw new IllegalArgumentException("Número de threads deve ser positivo");
        if (transacoes.isEmpty()) return new Acumulador().resultado();
        int tarefas = Math.min(numThreads, transacoes.size());
        int tamanhoFatia = (transacoes.size() + tarefas - 1) / tarefas;
        ExecutorService executor = Executors.newFixedThreadPool(tarefas);
        List<Future<Resultado>> resultados = new ArrayList<>();
        try {
            for (int i = 0; i < tarefas; i++) {
                final int inicio = i * tamanhoFatia;
                final int fim = Math.min(inicio + tamanhoFatia, transacoes.size());
                if (inicio >= fim) break;
                Callable<Resultado> tarefa = () -> {
                    Acumulador parcial = new Acumulador();
                    for (int indice = inicio; indice < fim; indice++) {
                        parcial.adicionar(transacoes.get(indice));
                    }
                    return parcial.resultado();    
                };
                resultados.add(executor.submit(tarefa));
            }

            Acumulador total = new Acumulador();
            for (Future<Resultado> futuro : resultados) total.combinar(futuro.get());
            return total.resultado();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("O cálculo paralelo foi interrompido.", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Falha em uma tarefa de agregação.", e.getCause());
        } finally {
            executor.shutdown();
        }
    }
        public void imprimirResultado(String titulo, Resultado resultado) {
        System.out.println("\n=== " + titulo + " ===");
        System.out.println("Transações processadas: " + resultado.quantidade);
        System.out.printf("Soma total: R$ %.2f%n", resultado.soma);
        System.out.printf("Valor médio: R$ %.2f%n", resultado.media());
        
        if (resultado.quantidade == 0) {
            System.out.println("Maior transação: não há dados");
            System.out.println("Menor transação: não há dados");
        } else {
            System.out.printf("Maior transação: R$ %.2f (ID %d)%n", resultado.maior.valor, resultado.maior.id);
            System.out.printf("Menor transação: R$ %.2f (ID %d)%n", resultado.menor.valor, resultado.menor.id);
        }
    }

    private void validarDados(List<Transacao> transacoes){
        if (transacoes == null) throw new IllegalArgumentException("A lista de transações não pode ser nula!");
    }
    private static class Acumulador { 
        private long quantidade;
        private double soma;
        private Transacao maior; 
        private Transacao menor; 
        
        private void adicionar(Transacao transacao){
            if (transacao == null) return;
            quantidade++;
            soma += transacao.valor;
            if (maior == null || transacao.valor > maior.valor ) maior = transacao;
            if (menor == null || transacao.valor < menor.valor ) menor = transacao;
             
        } 
        private void combinar(Resultado parcial) {
            quantidade += parcial.quantidade;
            soma+= parcial.soma;
            if (parcial.maior != null && (maior == null || parcial.maior.valor > maior.valor)) maior = parcial.maior;
            if (parcial.menor != null && (menor == null || parcial.menor.valor < menor.valor)) menor = parcial.menor;
        }
        private Resultado resultado(){
            return new Resultado(quantidade, soma, maior, menor);
        }
    }
}
