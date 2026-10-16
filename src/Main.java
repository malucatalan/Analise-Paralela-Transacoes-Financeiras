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
                    System.out.println("1. Agregação sequencial");
                    System.out.println("2. Agregação paralela (ExecutorService)");
                    System.out.print("Escolha a versão: ");
                    int versao = scanner.nextInt();
                    scanner.nextLine();
                    if (versao == 1) {
                        AgregacaoFinanceira.Resultado resultado = agregacaoFinanceira.calcularSequencial(dadosAtuais);
                        agregacaoFinanceira.imprimirResultado("AGREGAÇÃO FINANCEIRA SEQUENCIAL", resultado);
                    } else if (versao == 2) {
                        System.out.print("Número de threads: ");
                        int qtdThreads = scanner.nextInt();
                        scanner.nextLine();
                        AgregacaoFinanceira.Resultado resultado = agregacaoFinanceira.calcularParalelo(dadosAtuais, qtdThreads);
                        agregacaoFinanceira.imprimirResultado("AGREGAÇÃO FINANCEIRA PARALELA - " + qtdThreads + " THREADS", resultado);
                    } else {
                        System.out.println("Versão inválida.");
                    }
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
                    long inicioSeq = System.currentTimeMillis();
                    analiseSuspeitas.calcularSequencial(dadosAtuais);
                    long tempoSeq = System.currentTimeMillis() - inicioSeq;

                    // 2. Execução Paralela com medição de tempo
                    long inicioPar = System.currentTimeMillis();
                    analiseSuspeitas.calcularParalelo(dadosAtuais, qtdThreads);
                    long tempoPar = System.currentTimeMillis() - inicioPar;

                    // 3. Execução Paralela com Afinidade (Affinity) com medição de tempo
                    long inicioParAffinity = System.currentTimeMillis();
                    analiseSuspeitas.calcularParaleloComAffinity(dadosAtuais, qtdThreads);
                    long tempoParAffinity = System.currentTimeMillis() - inicioParAffinity;

                    // 4. Comparativo de Desempenho
                    double speedupPar = (double) tempoSeq / (tempoPar == 0 ? 1 : tempoPar);
                    double eficienciaPar = (speedupPar / qtdThreads) * 100;

                    double speedupAffinity = (double) tempoSeq / (tempoParAffinity == 0 ? 1 : tempoParAffinity);
                    double eficienciaAffinity = (speedupAffinity / qtdThreads) * 100;

                    System.out.println("==================================================");
                    System.out.println("          COMPARATIVO DE DESEMPENHO");
                    System.out.println("==================================================");
                    System.out.printf("  • Tempo Sequencial (1 thread)        : %,d ms\n", tempoSeq);
                    System.out.printf("  • Tempo Paralelo   (%d threads)      : %,d ms\n", qtdThreads, tempoPar);
                    System.out.printf("  • Tempo Affinity   (%d threads)      : %,d ms\n", qtdThreads, tempoParAffinity);
                    System.out.println("--------------------------------------------------");
                    System.out.printf("  • Speedup (Paralelo)                 : %.2fx mais rápido\n", speedupPar);
                    System.out.printf("  • Eficiência (Paralelo)              : %.1f%%\n", eficienciaPar);
                    System.out.printf("  • Speedup (Affinity)                 : %.2fx mais rápido\n", speedupAffinity);
                    System.out.printf("  • Eficiência (Affinity)              : %.1f%%\n", eficienciaAffinity);
                    System.out.println("==================================================\n");
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
                    double speedupPar = (double) resSeq.tempoExecucaoMs / (resPar.tempoExecucaoMs == 0 ? 1 : resPar.tempoExecucaoMs);
                    double eficienciaPar = (speedupPar / qtdThreads) * 100;

                    double speedupAffinity = (double) resSeq.tempoExecucaoMs / (resParAffinity.tempoExecucaoMs == 0 ? 1 : resParAffinity.tempoExecucaoMs);
                    double eficienciaAffinity = (speedupAffinity / qtdThreads) * 100;

                    System.out.println("==================================================");
                    System.out.println("          COMPARATIVO DE DESEMPENHO");
                    System.out.println("==================================================");
                    System.out.printf("  • Tempo Sequencial (1 thread)        : %,d ms\n", resSeq.tempoExecucaoMs);
                    System.out.printf("  • Tempo Paralelo   (%d threads)      : %,d ms\n", qtdThreads, resPar.tempoExecucaoMs);
                    System.out.printf("  • Tempo Affinity   (%d threads)      : %,d ms\n", qtdThreads, resParAffinity.tempoExecucaoMs);
                    System.out.println("--------------------------------------------------");
                    System.out.printf("  • Speedup (Paralelo)                 : %.2fx mais rápido\n", speedupPar);
                    System.out.printf("  • Eficiência (Paralelo)              : %.1f%%\n", eficienciaPar);
                    System.out.printf("  • Speedup (Affinity)                 : %.2fx mais rápido\n", speedupAffinity);
                    System.out.printf("  • Eficiência (Affinity)              : %.1f%%\n", eficienciaAffinity);
                    System.out.println("==================================================\n");
                }
                pausar(scanner);
            }
            // --- ESTATÍSTICAS POR CATEGORIA (PARALELO) ---
            else if (opcao == 6) {
                if (dadosAtuais.isEmpty()) {
                    System.out.println("Erro: Gere os dados primeiro (Opção 1).");
                } else {
                    System.out.print("Quantas threads deseja usar para o cálculo paralelo? (ex: 2, 4, 8, 16): ");
                    int qtdThreads = scanner.nextInt();
                    scanner.nextLine();
                    
                    System.out.println("Calculando estatísticas paralelamente com " + qtdThreads + " threads...\n");
                    analiseEstatistica.calcularParalelo(dadosAtuais, qtdThreads);
                }
                pausar(scanner);
            }
            else if (opcao == 7) {
                if (dadosAtuais.isEmpty()) {
                    System.out.println("Erro: Gere os dados primeiro (Opção 1).");
                } else {
                    System.out.println("Em breve");
                }
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
}