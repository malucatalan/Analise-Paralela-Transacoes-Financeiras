//SOURCES *.java
//DEPS net.openhft:affinity:3.23.3
import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        GeradorDeDados gerador = new GeradorDeDados();
        EstatisticasCategoria analiseEstatistica = new EstatisticasCategoria();
        TransacoesSuspeitas analiseSuspeitas = new TransacoesSuspeitas();
        AgregacaoFinanceira agregacaoFinanceira = new AgregacaoFinanceira();
        
        List<Transacao> dadosAtuais = new ArrayList<>();

        while (true) {
            limparTela();

            System.out.println("=== SISTEMA DE ANÁLISE DE TRANSAÇÕES ===");
            System.out.println("\n--- DADOS ---");
            System.out.println("1. Gerar novos dados");
            System.out.println("2. Visualizar amostra dos dados gerados");
            System.out.println("\n--- ANÁLISES DE NEGÓCIO ---");
            System.out.println("3. Cálculos de Transações");
            System.out.println("4. Detecção de Transações Suspeitas");
            System.out.println("5. Estatísticas por Categoria");
            System.out.println("\n--- BENCHMARK E RESULTADOS ---");
            System.out.println("6. Executar Benchmark Completo: Tempos, SpeedUp e Eficiência");
            System.out.println("7. Gerar Gráficos de Desempenho");
            System.out.println("\n0. Sair");
            System.out.print("Escolha uma opção: ");
            
            int opcao = scanner.nextInt();
            scanner.nextLine();

            if (opcao == 1) {
                System.out.print("Quantas transações deseja gerar? (ex: 1000000): ");
                int qtd = scanner.nextInt();
                scanner.nextLine();
                
                System.out.println("\nGerando " + qtd + " transações. Aguarde...");
                dadosAtuais = gerador.gerarTransacoes(qtd);
                System.out.println("Sucesso! Transações geradas na memória.");
                
                pausar(scanner);
            } 
            else if (opcao == 2) {
                if (dadosAtuais.isEmpty()) {
                    System.out.println("Erro: Não há dados. Gere os dados primeiro (Opção 1).");
                } else {
                    System.out.println("\n=== AMOSTRA DOS DADOS GERADOS ===");
                    int limite = Math.min(dadosAtuais.size(), 100); 
                    for (int i = 0; i < limite; i++) {
                        System.out.println(dadosAtuais.get(i).toString());
                    }
                    System.out.println("\n(Exibindo " + limite + " de " + dadosAtuais.size() + " transações totais)");
                }
                pausar(scanner);
            }
            else if (opcao == 3) {
                if (dadosAtuais.isEmpty()) {
                    System.out.println("Erro: Gere os dados primeiro (Opção 1).");
                } else {
                    System.out.print("Quantas threads deseja usar para a execução paralela? (ex: 2, 4, 8, 16): ");
                    int qtdThreads = scanner.nextInt();
                    scanner.nextLine();

                    System.out.println("\n==================================================");
                    System.out.println("          AGREGAÇÃO FINANCEIRA");
                    System.out.println("==================================================");

                    long inicioSeq = System.nanoTime();
                    AgregacaoFinanceira.Resultado resSeq = agregacaoFinanceira.calcularSequencial(dadosAtuais);
                    double tempoSeq = (System.nanoTime() - inicioSeq) / 1_000_000.0;

                    long inicioPar = System.nanoTime();
                    agregacaoFinanceira.calcularParalelo(dadosAtuais, qtdThreads);
                    double tempoPar = (System.nanoTime() - inicioPar) / 1_000_000.0;

                    long inicioParAffinity = System.nanoTime();
                    agregacaoFinanceira.calcularParaleloComAffinity(dadosAtuais, qtdThreads);
                    double tempoParAffinity = (System.nanoTime() - inicioParAffinity) / 1_000_000.0;

                    double speedupPar = tempoPar <= 0 ? 0 : tempoSeq / tempoPar;
                    double eficienciaPar = (speedupPar / qtdThreads) * 100;
                    double speedupAffinity = tempoParAffinity <= 0 ? 0 : tempoSeq / tempoParAffinity;
                    double eficienciaAffinity = (speedupAffinity / qtdThreads) * 100;

                    agregacaoFinanceira.imprimirResultado("RESULTADO DA AGREGAÇÃO", resSeq);
                    System.out.println("==================================================");
                    System.out.println("          COMPARATIVO DE DESEMPENHO");
                    System.out.println("==================================================");
                    System.out.printf("  • Tempo Sequencial (1 thread)        : %,.3f ms\n", tempoSeq);
                    System.out.printf("  • Tempo Paralelo   (%d threads)      : %,.3f ms\n", qtdThreads, tempoPar);
                    System.out.printf("  • Tempo Affinity   (%d threads)      : %,.3f ms\n", qtdThreads, tempoParAffinity);
                    System.out.println("--------------------------------------------------");
                    System.out.printf("  • Speedup (Paralelo)                 : %.2fx mais rápido\n", speedupPar);
                    System.out.printf("  • Eficiência (Paralelo)              : %.1f%%\n", eficienciaPar);
                    System.out.printf("  • Speedup (Affinity)                 : %.2fx mais rápido\n", speedupAffinity);
                    System.out.printf("  • Eficiência (Affinity)              : %.1f%%\n", eficienciaAffinity);
                    System.out.println("==================================================\n");

                    List<HistoricoDesempenho.Medicao> medicoes = new ArrayList<>();
                    medicoes.add(new HistoricoDesempenho.Medicao("Sequencial", 1, tempoSeq));
                    medicoes.add(new HistoricoDesempenho.Medicao("ExecutorService", qtdThreads, tempoPar));
                    medicoes.add(new HistoricoDesempenho.Medicao("Affinity", qtdThreads, tempoParAffinity));
                    HistoricoDesempenho.registrarGrupo("Agregacao", dadosAtuais.size(), medicoes);
                }
                pausar(scanner);
            }

            else if (opcao == 4) {
                if (dadosAtuais.isEmpty()) {
                    System.out.println("Erro: Gere os dados primeiro (Opção 1).");
                } else {
                    System.out.print("Quantas threads deseja usar para a execução paralela? (ex: 2, 4, 8, 16): ");
                    int qtdThreads = scanner.nextInt();
                    scanner.nextLine();

                    System.out.println("\n==================================================");
                    System.out.println("    DETECÇÃO DE TRANSAÇÕES SUSPEITAS");
                    System.out.println("==================================================");

                    // 1. Execução Sequencial com medição de tempo
                    long inicioSeq = System.nanoTime();
                    analiseSuspeitas.calcularSequencial(dadosAtuais);
                    double tempoSeq = (System.nanoTime() - inicioSeq) / 1_000_000.0;

                    // 2. Execução Paralela com medição de tempo
                    long inicioPar = System.nanoTime();
                    analiseSuspeitas.calcularParalelo(dadosAtuais, qtdThreads);
                    double tempoPar = (System.nanoTime() - inicioPar) / 1_000_000.0;

                    // 3. Execução Paralela com Afinidade (Affinity) com medição de tempo
                    long inicioParAffinity = System.nanoTime();
                    analiseSuspeitas.calcularParaleloComAffinity(dadosAtuais, qtdThreads);
                    double tempoParAffinity = (System.nanoTime() - inicioParAffinity) / 1_000_000.0;

                    // 4. Comparativo de Desempenho
                    double speedupPar = tempoPar <= 0 ? 0 : tempoSeq / tempoPar;
                    double eficienciaPar = (speedupPar / qtdThreads) * 100;

                    double speedupAffinity = tempoParAffinity <= 0 ? 0 : tempoSeq / tempoParAffinity;
                    double eficienciaAffinity = (speedupAffinity / qtdThreads) * 100;

                    System.out.println("==================================================");
                    System.out.println("          COMPARATIVO DE DESEMPENHO");
                    System.out.println("==================================================");
                    System.out.printf("  • Tempo Sequencial (1 thread)        : %,.3f ms\n", tempoSeq);
                    System.out.printf("  • Tempo Paralelo   (%d threads)      : %,.3f ms\n", qtdThreads, tempoPar);
                    System.out.printf("  • Tempo Affinity   (%d threads)      : %,.3f ms\n", qtdThreads, tempoParAffinity);
                    System.out.println("--------------------------------------------------");
                    System.out.printf("  • Speedup (Paralelo)                 : %.2fx mais rápido\n", speedupPar);
                    System.out.printf("  • Eficiência (Paralelo)              : %.1f%%\n", eficienciaPar);
                    System.out.printf("  • Speedup (Affinity)                 : %.2fx mais rápido\n", speedupAffinity);
                    System.out.printf("  • Eficiência (Affinity)              : %.1f%%\n", eficienciaAffinity);
                    System.out.println("==================================================\n");

                    List<HistoricoDesempenho.Medicao> medicoes = new ArrayList<>();
                    medicoes.add(new HistoricoDesempenho.Medicao("Sequencial", 1, tempoSeq));
                    medicoes.add(new HistoricoDesempenho.Medicao("ExecutorService", qtdThreads, tempoPar));
                    medicoes.add(new HistoricoDesempenho.Medicao("Affinity", qtdThreads, tempoParAffinity));
                    HistoricoDesempenho.registrarGrupo("Suspeitas", dadosAtuais.size(), medicoes);
                }
                pausar(scanner);
            }

            // --- ESTATÍSTICAS POR CATEGORIA (SEQUENCIAL + PARALELO) ---
            else if (opcao == 5) {
                if (dadosAtuais.isEmpty()) {
                    System.out.println("Erro: Gere os dados primeiro (Opção 1).");
                } else {
                    System.out.print("Quantas threads deseja usar para a execução paralela? (ex: 2, 4, 8, 16): ");
                    int qtdThreads = scanner.nextInt();
                    scanner.nextLine();

                    System.out.println("\n==================================================");
                    System.out.println("    ESTATÍSTICAS POR CATEGORIA");
                    System.out.println("==================================================");

                    // 1. Execução Sequencial
                    EstatisticasCategoria.ResultadoFinalCategorias resSeq = analiseEstatistica.calcularSequencial(dadosAtuais);

                    // 2. Execução Paralela (ExecutorService padrão)
                    EstatisticasCategoria.ResultadoFinalCategorias resPar = analiseEstatistica.calcularParalelo(dadosAtuais, qtdThreads);

                    // 3. Execução Paralela com Afinidade (Affinity)
                    EstatisticasCategoria.ResultadoFinalCategorias resParAffinity = analiseEstatistica.calcularParaleloComAffinity(dadosAtuais, qtdThreads);

                    // 4. Comparativo de Desempenho
                    double speedupPar = resPar.tempoExecucaoMs <= 0 ? 0 : resSeq.tempoExecucaoMs / resPar.tempoExecucaoMs;
                    double eficienciaPar = (speedupPar / qtdThreads) * 100;

                    double speedupAffinity = resParAffinity.tempoExecucaoMs <= 0 ? 0 : resSeq.tempoExecucaoMs / resParAffinity.tempoExecucaoMs;
                    double eficienciaAffinity = (speedupAffinity / qtdThreads) * 100;

                    System.out.println("==================================================");
                    System.out.println("          COMPARATIVO DE DESEMPENHO");
                    System.out.println("==================================================");
                    System.out.printf("  • Tempo Sequencial (1 thread)        : %,.3f ms\n", resSeq.tempoExecucaoMs);
                    System.out.printf("  • Tempo Paralelo   (%d threads)      : %,.3f ms\n", qtdThreads, resPar.tempoExecucaoMs);
                    System.out.printf("  • Tempo Affinity   (%d threads)      : %,.3f ms\n", qtdThreads, resParAffinity.tempoExecucaoMs);
                    System.out.println("--------------------------------------------------");
                    System.out.printf("  • Speedup (Paralelo)                 : %.2fx mais rápido\n", speedupPar);
                    System.out.printf("  • Eficiência (Paralelo)              : %.1f%%\n", eficienciaPar);
                    System.out.printf("  • Speedup (Affinity)                 : %.2fx mais rápido\n", speedupAffinity);
                    System.out.printf("  • Eficiência (Affinity)              : %.1f%%\n", eficienciaAffinity);
                    System.out.println("==================================================\n");

                    List<HistoricoDesempenho.Medicao> medicoes = new ArrayList<>();
                    medicoes.add(new HistoricoDesempenho.Medicao("Sequencial", 1, resSeq.tempoExecucaoMs));
                    medicoes.add(new HistoricoDesempenho.Medicao("ExecutorService", qtdThreads, resPar.tempoExecucaoMs));
                    medicoes.add(new HistoricoDesempenho.Medicao("Affinity", qtdThreads, resParAffinity.tempoExecucaoMs));
                    HistoricoDesempenho.registrarGrupo("Categorias", dadosAtuais.size(), medicoes);
                }
                pausar(scanner);
            }
            // --- ESTATÍSTICAS POR CATEGORIA (PARALELO) ---
            else if (opcao == 6) {
                if (dadosAtuais.isEmpty()) {
                    System.out.println("Erro: Gere os dados primeiro (Opção 1).");
                } else {
                    executarBenchmarkCompleto(dadosAtuais);
                }
                pausar(scanner);
            }
            else if (opcao == 7) {
                HistoricoDesempenho.exibirGraficos();
                pausar(scanner);
            }
            else if (opcao == 8) {
                System.out.println("Em breve");
                pausar(scanner);
            }
            else if (opcao == 0) {
                System.out.println("Encerrando o sistema...");
                break;
            }
            else {
                System.out.println("Opção inválida!");
                pausar(scanner);
            }
        }
        scanner.close();
    }

    public static void limparTela() {
        System.out.print("\033[H\033[2J");  
        System.out.flush();  
    }

    public static void pausar(Scanner scanner) {
        System.out.println("\nPressione Enter para voltar ao menu...");
        scanner.nextLine();
    }

    private static void executarBenchmarkCompleto(List<Transacao> dados) {
        int[] configuracoesThreads = {1, 2, 4, 8, 16};
        AgregacaoFinanceira agregacao = new AgregacaoFinanceira();
        List<HistoricoDesempenho.Medicao> medicoes = new ArrayList<>();

        double tempoSequencial = medirMilissegundos(() -> agregacao.calcularSequencial(dados));
        medicoes.add(new HistoricoDesempenho.Medicao("Sequencial", 1, tempoSequencial));

        System.out.println("\n=== BENCHMARK COMPLETO DE AGREGAÇÃO ===");
        System.out.printf("%-12s %14s %12s %14s%n", "Threads", "Tempo (ms)", "Speedup", "Eficiência");
        imprimirLinhaBenchmark(1, tempoSequencial, tempoSequencial);

        for (int threads : configuracoesThreads) {
            if (threads == 1) continue;
            final int qtdThreads = threads;
            double tempo = medirMilissegundos(() -> agregacao.calcularParalelo(dados, qtdThreads));
            medicoes.add(new HistoricoDesempenho.Medicao("ExecutorService", threads, tempo));
            imprimirLinhaBenchmark(threads, tempo, tempoSequencial);
        }

        HistoricoDesempenho.registrarGrupo("Benchmark Agregacao", dados.size(), medicoes);
    }

    private static void imprimirLinhaBenchmark(int threads, double tempo, double tempoSequencial) {
        double speedup = tempo <= 0.0 ? 0.0 : tempoSequencial / tempo;
        double eficiencia = threads <= 0 ? 0.0 : speedup / threads * 100.0;
        System.out.printf("%-12d %14.3f %11.2fx %13.1f%%%n", threads, tempo, speedup, eficiencia);
    }

    private static double medirMilissegundos(Runnable tarefa) {
        long inicio = System.nanoTime();
        tarefa.run();
        return (System.nanoTime() - inicio) / 1_000_000.0;
    }
}
