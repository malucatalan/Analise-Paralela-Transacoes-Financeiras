import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class EstatisticasCategoria {

    class ResumoCategoria {
        int quantidade = 0;
        double valorTotal = 0.0;
        double maiorTransacao = 0.0;
        double menorTransacao = Double.MAX_VALUE; 
        List<Double> todosOsValores = new ArrayList<>();
    }

    public void calcularSequencial(List<Transacao> transacoes) {
        // Prepara as 5 categorias zeradas
        Map<String, ResumoCategoria> mapaEstatisticas = inicializarMapa();

        // O computador lê a lista inteira do início ao fim
        for (Transacao t : transacoes) {
            processarTransacao(mapaEstatisticas, t);
        }

        System.out.println("\n[RESULTADO SEQUENCIAL - 1 THREAD]");
        imprimirTabela(mapaEstatisticas);
    }

    public void calcularParalelo(List<Transacao> transacoes, int numThreads) {
        
        // EXECUTOR: contrata o número exato de threads 
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        
        // FUTURE: É uma lista de "promessas". As threads vão guardar aqui o resultado delas quando terminarem
        List<Future<Map<String, ResumoCategoria>>> resultados = new ArrayList<>();
        
        // Calcula o tamanho da fatia da lista (ex: 10 milhões de dados / 4 threads = 2,5 milhões por fatia)
        int tamanhoFatia = transacoes.size() / numThreads;
        
        // Criação e distribuição das tarefas
        for (int i = 0; i < numThreads; i++) {
            int inicio = i * tamanhoFatia; // Ponto de corte inicial da fatia
            
            // Ponto de corte final. Se for a última thread, ela pega tudo o que sobrou até o fim da lista
            int fim = (i == numThreads - 1) ? transacoes.size() : (i + 1) * tamanhoFatia;
            
            // Separa a fatia exata dos dados e cria a tarefa para a thread resolver
            Callable<Map<String, ResumoCategoria>> tarefa = new TarefaEstatisticas(transacoes.subList(inicio, fim));
            
            // Entrega a tarefa ao executor e guarda a "promessa" de resultado
            resultados.add(executor.submit(tarefa));
        }

        // Mapa principal vazio onde junta as respostas de todas as threads no final
        Map<String, ResumoCategoria> mapaFinal = inicializarMapa();

        try {
            // O loop que junta os pedaços separados num resultado único
            for (Future<Map<String, ResumoCategoria>> resultado : resultados) {
                
                // O .get() faz o código esperar até que a thread termine e entregue o mapa dela
                Map<String, ResumoCategoria> mapaParcial = resultado.get();
                
                // Pega os valores que a thread calculou e soma no mapa principal
                for (String categoria : mapaFinal.keySet()) {
                    ResumoCategoria fimResumo = mapaFinal.get(categoria); 
                    ResumoCategoria parcResumo = mapaParcial.get(categoria); 
                    
                    fimResumo.quantidade += parcResumo.quantidade;
                    fimResumo.valorTotal += parcResumo.valorTotal;
                    fimResumo.todosOsValores.addAll(parcResumo.todosOsValores);
                    
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

        System.out.println("\n[RESULTADO PARALELO - " + numThreads + " THREADS]");
        imprimirTabela(mapaFinal);
    }

    // Callable é uma interface que obriga a thread a devolver um resultado no final (o Map)
    class TarefaEstatisticas implements Callable<Map<String, ResumoCategoria>> {
        private List<Transacao> fatia;

        // Construtor: A thread recebe a sua fatia exclusiva de dados
        public TarefaEstatisticas(List<Transacao> fatia) {
            this.fatia = fatia;
        }

        // O método call() é o que a thread efetivamente executa quando começa a rodar
        @Override
        public Map<String, ResumoCategoria> call() {
            // Cria um mapa zerado que pertence APENAS a esta thread (para elas não brigarem pela mesma variável)
            Map<String, ResumoCategoria> mapaLocal = inicializarMapa();
            
            // Calcula apenas os dados da fatia dela
            for (Transacao t : fatia) {
                processarTransacao(mapaLocal, t);
            }
            
            // Devolve o mapa preenchido
            return mapaLocal;
        }
    }
    
    // Método para criar as categorias zeradas rapidamente
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
            resumo.todosOsValores.add(t.valor); 
            if (t.valor > resumo.maiorTransacao) resumo.maiorTransacao = t.valor;
            if (t.valor < resumo.menorTransacao) resumo.menorTransacao = t.valor;
        }
    }

    private void imprimirTabela(Map<String, ResumoCategoria> mapaEstatisticas) {
        System.out.printf("\n%-16s %10s %20s %18s %18s %18s %18s\n", 
                          "Categoria", "Quantidade", "Total", "Média", "Mediana", "Menor Trans.", "Maior Trans.");
        System.out.println("-----------------------------------------------------------------------------------------------------------------------------");
        
        for (String nomeCategoria : mapaEstatisticas.keySet()) {
            ResumoCategoria res = mapaEstatisticas.get(nomeCategoria);
            
            if (res.quantidade > 0) {
                double media = res.valorTotal / res.quantidade;
                Collections.sort(res.todosOsValores);
                double mediana = 0;
                int meio = res.quantidade / 2;
                if (res.quantidade % 2 == 0) {
                    mediana = (res.todosOsValores.get(meio - 1) + res.todosOsValores.get(meio)) / 2.0;
                } else {
                    mediana = res.todosOsValores.get(meio);
                }
                
                System.out.printf("%-16s %,10d    R$ %,14.2f   R$ %,13.2f   R$ %,13.2f   R$ %,13.2f   R$ %,13.2f\n", 
                                  nomeCategoria, res.quantidade, res.valorTotal, media, mediana, res.menorTransacao, res.maiorTransacao);
            }
        }
        System.out.println("-----------------------------------------------------------------------------------------------------------------------------\n");
    }
}