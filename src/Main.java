import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        GeradorDeDados gerador = new GeradorDeDados();
        EstatisticasCategoria analiseEstatistica = new EstatisticasCategoria();
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
            System.out.println("5. Estatísticas por Categoria - Sequencial");
            System.out.println("6. Estatísticas por Categoria - Paralelo");
            System.out.println("\n--- BENCHMARK E RESULTADOS ---");
            System.out.println("7. Executar Benchmark: Tempos, SpeedUp e Eficiência");
            System.out.println("8. Gerar Gráficos de Desempenho");
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
                    System.out.println("Em breve");
                }
                pausar(scanner);
            }
            else if (opcao == 4) {
                if (dadosAtuais.isEmpty()) {
                    System.out.println("Erro: Gere os dados primeiro (Opção 1).");
                } else {
                    System.out.println("Em breve");
                }
                pausar(scanner);
            }
            else if (opcao == 5) {
                if (dadosAtuais.isEmpty()) {
                    System.out.println("Erro: Gere os dados primeiro (Opção 1).");
                } else {
                    System.out.println("Calculando estatísticas...\n");
                    analiseEstatistica.calcularSequencial(dadosAtuais);
                }
                pausar(scanner);
            }
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