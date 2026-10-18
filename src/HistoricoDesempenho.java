import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

/** Guarda medições entre execuções e exibe gráficos com todo o histórico. */
public final class HistoricoDesempenho {
    private static final Path ARQUIVO_CSV = Paths.get("resultados_benchmark.csv");
    private static final Path ARQUIVO_SVG = Paths.get("graficos_desempenho.svg");
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static JFrame janelaGraficos;

    private HistoricoDesempenho() { }

    public static final class Medicao {
        public final String metodo;
        public final int threads;
        public final double tempoMs;

        public Medicao(String metodo, int threads, double tempoMs) {
            this.metodo = metodo;
            this.threads = threads;
            this.tempoMs = tempoMs;
        }
    }

    private static final class Registro {
        String execucao;
        String dataHora;
        String analise;
        int transacoes;
        int threads;
        String metodo;
        double tempoMs;
        double speedup;
        double eficiencia;
        boolean media;
        int quantidadeTestes;
    }

    public static void registrarGrupo(String analise, int transacoes, List<Medicao> medicoes) {
        if (medicoes == null || medicoes.isEmpty()) return;

        double tempoSequencial = medicoes.get(0).tempoMs;
        String execucao = UUID.randomUUID().toString();
        String dataHora = LocalDateTime.now().format(FORMATO_DATA);
        boolean arquivoNovo = !Files.exists(ARQUIVO_CSV);

        try (BufferedWriter writer = Files.newBufferedWriter(
                ARQUIVO_CSV,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND)) {
            if (arquivoNovo) {
                writer.write("execucao;data_hora;analise;transacoes;threads;metodo;tempo_ms;speedup;eficiencia_pct");
                writer.newLine();
            }

            for (Medicao medicao : medicoes) {
                double speedup = medicao.tempoMs <= 0.0 ? 0.0 : tempoSequencial / medicao.tempoMs;
                double eficiencia = medicao.threads <= 0 ? 0.0 : speedup / medicao.threads * 100.0;
                writer.write(String.format(Locale.US, "%s;%s;%s;%d;%d;%s;%.6f;%.6f;%.4f",
                        execucao, dataHora, analise, transacoes, medicao.threads,
                        medicao.metodo, medicao.tempoMs, speedup, eficiencia));
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("Não foi possível salvar o histórico: " + e.getMessage());
            return;
        }

        exibirGraficos();
    }

    public static void exibirGraficos() {
        List<Registro> registros;
        try {
            registros = lerHistorico();
        } catch (IOException e) {
            System.out.println("Não foi possível ler o histórico: " + e.getMessage());
            return;
        }

        if (registros.isEmpty()) {
            System.out.println("Ainda não há resultados salvos. Execute uma análise primeiro.");
            return;
        }

        Map<String, List<Registro>> porRegra = agruparPorRegra(registros);
        for (Map.Entry<String, List<Registro>> grupo : porRegra.entrySet()) {
            imprimirGrafico(grupo.getKey() + " — TEMPO DE EXECUÇÃO (ms)", grupo.getValue(), "tempo");
            imprimirGrafico(grupo.getKey() + " — SPEEDUP", grupo.getValue(), "speedup");
            imprimirGrafico(grupo.getKey() + " — EFICIÊNCIA (%)", grupo.getValue(), "eficiencia");
        }

        try {
            gravarSvg(registros);
            System.out.println("Gráfico visual salvo em: " + ARQUIVO_SVG.toAbsolutePath());
            System.out.println("Dados acumulados salvos em: " + ARQUIVO_CSV.toAbsolutePath());
        } catch (IOException e) {
            System.out.println("Não foi possível gerar o arquivo SVG: " + e.getMessage());
        }
        mostrarGraficosSwing(registros);
    }

    private static void mostrarGraficosSwing(List<Registro> registros) {
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("Ambiente sem interface gráfica; os gráficos estão no console e no arquivo SVG.");
            return;
        }

        Map<String, List<Registro>> porRegra = agruparPorRegra(registros);
        SwingUtilities.invokeLater(() -> {
            if (janelaGraficos == null || !janelaGraficos.isDisplayable()) {
                janelaGraficos = new JFrame("Histórico de desempenho das análises");
                janelaGraficos.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                janelaGraficos.setSize(1120, 740);
                janelaGraficos.setLocationRelativeTo(null);
            }

            JTabbedPane abasRegras = new JTabbedPane();
            for (Map.Entry<String, List<Registro>> grupo : porRegra.entrySet()) {
                List<Registro> dadosRegra = new ArrayList<>(grupo.getValue());
                JTabbedPane abasMetricas = new JTabbedPane();

                GraficoPainel painelTempo = new GraficoPainel("Tempo de execução (ms)", "tempo", new Color(37, 99, 235));
                GraficoPainel painelSpeedup = new GraficoPainel("Speedup", "speedup", new Color(22, 163, 74));
                GraficoPainel painelEficiencia = new GraficoPainel("Eficiência (%)", "eficiencia", new Color(234, 88, 12));
                painelTempo.atualizar(dadosRegra);
                painelSpeedup.atualizar(dadosRegra);
                painelEficiencia.atualizar(dadosRegra);

                abasMetricas.addTab("Tempo", new JScrollPane(painelTempo));
                abasMetricas.addTab("Speedup", new JScrollPane(painelSpeedup));
                abasMetricas.addTab("Eficiência", new JScrollPane(painelEficiencia));
                abasRegras.addTab(grupo.getKey(), abasMetricas);
            }
            janelaGraficos.setContentPane(abasRegras);
            janelaGraficos.revalidate();
            janelaGraficos.setVisible(true);
            janelaGraficos.setState(JFrame.NORMAL);
            janelaGraficos.toFront();
        });
    }

    private static Map<String, List<Registro>> agruparPorRegra(List<Registro> registros) {
        Map<String, List<Registro>> grupos = new LinkedHashMap<>();
        String[] ordem = {"Agregação Financeira", "Transações Suspeitas", "Estatísticas por Categoria"};
        for (String regra : ordem) grupos.put(regra, new ArrayList<>());
        for (Registro registro : registros) {
            String regra = nomeDaRegra(registro.analise);
            grupos.computeIfAbsent(regra, ignorado -> new ArrayList<>()).add(registro);
        }
        grupos.entrySet().removeIf(grupo -> grupo.getValue().isEmpty());
        return grupos;
    }

    private static String nomeDaRegra(String analise) {
        String normalizada = analise.toLowerCase(Locale.ROOT);
        if (normalizada.contains("agreg")) return "Agregação Financeira";
        if (normalizada.contains("suspeit")) return "Transações Suspeitas";
        if (normalizada.contains("categoria")) return "Estatísticas por Categoria";
        return analise;
    }

    private static final class GraficoPainel extends JPanel {
        private final String titulo;
        private final String metrica;
        private final Color cor;
        private List<Registro> registros = new ArrayList<>();
        private List<Registro> pontosGrafico = new ArrayList<>();
        private int[] numeracaoExecucoes = new int[0];

        GraficoPainel(String titulo, String metrica, Color cor) {
            this.titulo = titulo;
            this.metrica = metrica;
            this.cor = cor;
            setBackground(Color.WHITE);
        }

        void atualizar(List<Registro> novosRegistros) {
            registros = new ArrayList<>(novosRegistros);
            numeracaoExecucoes = numerarExecucoes(registros);
            pontosGrafico = new ArrayList<>(registros);
            pontosGrafico.addAll(calcularMedias(registros));
            setPreferredSize(new Dimension(1080, Math.max(360, 170 + pontosGrafico.size() * 35)));
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(15, 23, 42));
            g.setFont(new Font("Segoe UI", Font.BOLD, 20));
            g.drawString("Histórico de desempenho — " + titulo, 20, 28);
            g.setColor(cor);
            g.drawLine(20, 38, getWidth() - 20, 38);

            List<Registro> ordenados = new ArrayList<>(pontosGrafico);
            Comparator<Registro> ordem = Comparator.comparingDouble(r -> obterValor(r, metrica));
            if (!"tempo".equals(metrica)) ordem = ordem.reversed();
            ordenados.sort(ordem);

            Map<String, Integer> numeroPorExecucao = new HashMap<>();
            for (int i = 0; i < registros.size(); i++) {
                numeroPorExecucao.put(registros.get(i).execucao, numeracaoExecucoes[i]);
            }

            int inicioBarras = 445;
            int colunaDiferenca = getWidth() - 105;
            int fimBarras = colunaDiferenca - 24;
            int larguraBarras = Math.max(100, fimBarras - inicioBarras);
            int topo = 98;
            int alturaLinha = 35;
            int eixoY = topo + ordenados.size() * alturaLinha + 14;
            double maximo = 0.0;
            for (Registro registro : ordenados) maximo = Math.max(maximo, obterValor(registro, metrica));
            if (maximo <= 0.0) maximo = 1.0;

            g.setFont(new Font("Segoe UI", Font.BOLD, 12));
            g.setColor(new Color(55, 65, 81));
            g.drawString("Execução / configuração", 20, 78);
            g.drawString(titulo, inicioBarras, 78);
            g.drawString("Diferença", colunaDiferenca, 78);
            g.drawLine(20, 85, getWidth() - 20, 85);
            g.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            g.drawString("Vermelho: melhor execução   Azul: média dos testes   Laranja: teste individual", 20, 50);

            for (int tick = 0; tick <= 5; tick++) {
                int x = inicioBarras + larguraBarras * tick / 5;
                double valorEixo = maximo * tick / 5.0;
                g.setColor(new Color(226, 232, 240));
                g.drawLine(x, topo - 6, x, eixoY);
                String textoEixo = formatarValor(valorEixo, metrica);
                int larguraTexto = g.getFontMetrics().stringWidth(textoEixo);
                g.setColor(new Color(71, 85, 105));
                g.drawString(textoEixo, x - larguraTexto / 2, eixoY + 19);
            }

            Registro melhorExecucao = encontrarMelhor(registros, metrica);
            double melhor = obterValor(melhorExecucao, metrica);
            for (int i = 0; i < ordenados.size(); i++) {
                Registro registro = ordenados.get(i);
                int y = topo + i * alturaLinha;
                double valor = obterValor(registro, metrica);
                int largura = (int) Math.round(valor / maximo * larguraBarras);
                int numero = numeroPorExecucao.getOrDefault(registro.execucao, 1);
                String rotulo = registro.media
                        ? String.format(Locale.ROOT, "Média de %d testes | %s | %s | %dt | %d tx",
                                registro.quantidadeTestes, nomeDaRegra(registro.analise), registro.metodo,
                                registro.threads, registro.transacoes)
                        : String.format(Locale.ROOT, "R%d | %s | %s | %dt | %d tx",
                                numero, nomeDaRegra(registro.analise), registro.metodo, registro.threads,
                                registro.transacoes);

                g.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                g.setColor(new Color(31, 41, 55));
                g.drawString(ajustarTexto(rotulo, g, inicioBarras - 32), 20, y + 18);

                Color corBarra = registro.media ? new Color(0, 0, 235)
                        : registro == melhorExecucao ? new Color(239, 0, 0)
                        : new Color(255, 153, 0);
                g.setColor(corBarra);
                g.fillRect(inicioBarras, y, largura, 25);

                String textoValor = formatarValor(valor, metrica);
                g.setColor(registro.media || registro == melhorExecucao
                        ? Color.WHITE : new Color(31, 41, 55));
                if (largura >= g.getFontMetrics().stringWidth(textoValor) + 14) {
                    g.drawString(textoValor, inicioBarras + largura - g.getFontMetrics().stringWidth(textoValor) - 7, y + 18);
                } else {
                    g.setColor(new Color(31, 41, 55));
                    g.drawString(textoValor, inicioBarras + largura + 6, y + 18);
                }

                double diferenca = calcularDiferencaPercentual(valor, melhor);
                g.setColor(new Color(239, 0, 0));
                g.setFont(new Font("Segoe UI", Font.BOLD, 13));
                g.drawString(String.format(Locale.ROOT, "%+.1f%%", diferenca), colunaDiferenca, y + 18);
            }

            g.setColor(new Color(71, 85, 105));
            g.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            g.drawString("Melhor resultado: " + ("tempo".equals(metrica) ? "menor é melhor" : "maior é melhor"),
                    20, eixoY + 43);
            g.dispose();
        }
    }

    private static double calcularDiferencaPercentual(double valor, double melhor) {
        if (melhor == 0.0) return 0.0;
        return (valor / melhor - 1.0) * 100.0;
    }

    private static String formatarValor(double valor, String metrica) {
        if ("tempo".equals(metrica)) {
            return String.format(Locale.ROOT, valor >= 100.0 ? "%.0f" : "%.3f", valor);
        }
        return "speedup".equals(metrica)
                ? String.format(Locale.ROOT, "%.2f", valor)
                : String.format(Locale.ROOT, "%.1f", valor);
    }

    private static String ajustarTexto(String texto, Graphics2D g, int larguraMaxima) {
        if (g.getFontMetrics().stringWidth(texto) <= larguraMaxima) return texto;
        while (!texto.isEmpty() && g.getFontMetrics().stringWidth(texto + "...") > larguraMaxima) {
            texto = texto.substring(0, texto.length() - 1);
        }
        return texto + "...";
    }

    private static List<Registro> calcularMedias(List<Registro> registros) {
        Map<String, AcumuladorMedia> grupos = new LinkedHashMap<>();
        for (Registro registro : registros) {
            String regra = nomeDaRegra(registro.analise);
            String chave = regra + "|" + registro.metodo + "|"
                    + registro.threads + "|" + registro.transacoes;
            AcumuladorMedia acumulador = grupos.get(chave);
            if (acumulador == null) {
                acumulador = new AcumuladorMedia(registro);
                grupos.put(chave, acumulador);
            }
            acumulador.adicionar(registro);
        }

        List<Registro> medias = new ArrayList<>();
        for (AcumuladorMedia acumulador : grupos.values()) {
            // A média só acrescenta uma linha útil depois de pelo menos duas execuções comparáveis.
            if (acumulador.quantidade < 2) continue;
            Registro media = new Registro();
            media.execucao = "MEDIA-" + nomeDaRegra(acumulador.modelo.analise) + "-"
                    + acumulador.modelo.metodo + "-" + acumulador.modelo.threads
                    + "-" + acumulador.modelo.transacoes;
            media.dataHora = acumulador.modelo.dataHora;
            media.analise = nomeDaRegra(acumulador.modelo.analise);
            media.transacoes = acumulador.modelo.transacoes;
            media.threads = acumulador.modelo.threads;
            media.metodo = acumulador.modelo.metodo;
            media.tempoMs = acumulador.tempo / acumulador.quantidade;
            media.speedup = acumulador.speedup / acumulador.quantidade;
            media.eficiencia = acumulador.eficiencia / acumulador.quantidade;
            media.media = true;
            media.quantidadeTestes = acumulador.quantidade;
            medias.add(media);
        }
        return medias;
    }

    private static Registro encontrarMelhor(List<Registro> registros, String metrica) {
        Registro melhor = registros.get(0);
        for (int i = 1; i < registros.size(); i++) {
            Registro candidato = registros.get(i);
            double valorCandidato = obterValor(candidato, metrica);
            double valorMelhor = obterValor(melhor, metrica);
            boolean menorMelhor = "tempo".equals(metrica);
            if (menorMelhor ? valorCandidato < valorMelhor : valorCandidato > valorMelhor) {
                melhor = candidato;
            }
        }
        return melhor;
    }

    private static final class AcumuladorMedia {
        final Registro modelo;
        int quantidade;
        double tempo;
        double speedup;
        double eficiencia;

        AcumuladorMedia(Registro modelo) {
            this.modelo = modelo;
        }

        void adicionar(Registro registro) {
            quantidade++;
            tempo += registro.tempoMs;
            speedup += registro.speedup;
            eficiencia += registro.eficiencia;
        }
    }

    private static List<Registro> lerHistorico() throws IOException {
        List<Registro> registros = new ArrayList<>();
        if (!Files.exists(ARQUIVO_CSV)) return registros;

        try (BufferedReader reader = Files.newBufferedReader(ARQUIVO_CSV, StandardCharsets.UTF_8)) {
            String linha = reader.readLine(); // cabeçalho
            while ((linha = reader.readLine()) != null) {
                String[] campos = linha.split(";", -1);
                if (campos.length != 9) continue;
                try {
                    Registro registro = new Registro();
                    registro.execucao = campos[0];
                    registro.dataHora = campos[1];
                    registro.analise = campos[2];
                    registro.transacoes = Integer.parseInt(campos[3]);
                    registro.threads = Integer.parseInt(campos[4]);
                    registro.metodo = campos[5];
                    registro.tempoMs = Double.parseDouble(campos[6]);
                    registro.speedup = Double.parseDouble(campos[7]);
                    registro.eficiencia = Double.parseDouble(campos[8]);
                    registros.add(registro);
                } catch (NumberFormatException ignorado) {
                    // Ignora linhas incompletas sem descartar as medições válidas.
                }
            }
        }
        return registros;
    }

    private static void imprimirGrafico(String titulo, List<Registro> registros, String metrica) {
        double maximo = valorMaximo(registros, metrica);
        int[] numeracaoExecucoes = numerarExecucoes(registros);
        System.out.println("\n=== " + titulo + " ===");
        System.out.println("# representa o valor relativo ao maior resultado deste gráfico.");

        for (int i = 0; i < registros.size(); i++) {
            Registro registro = registros.get(i);
            double valor = obterValor(registro, metrica);
            int tamanhoBarra = maximo == 0.0 ? 0 : (int) Math.round(valor / maximo * 36.0);
            String barra = "#".repeat(Math.max(0, tamanhoBarra));
            String rotulo = String.format(Locale.ROOT, "R%-3d %-22s %-20s %2d th",
                    numeracaoExecucoes[i], registro.analise, registro.metodo, registro.threads);
            System.out.printf(Locale.ROOT, "%-59s | %s %.3f%n", rotulo, barra, valor);
        }
    }

    private static double valorMaximo(List<Registro> registros, String metrica) {
        double maximo = 0.0;
        for (Registro registro : registros) maximo = Math.max(maximo, obterValor(registro, metrica));
        return maximo;
    }

    private static double obterValor(Registro registro, String metrica) {
        if ("speedup".equals(metrica)) return registro.speedup;
        if ("eficiencia".equals(metrica)) return registro.eficiencia;
        return registro.tempoMs;
    }

    private static void gravarSvg(List<Registro> registros) throws IOException {
        int largura = 1200;
        int alturaPainel = Math.max(190, 76 + registros.size() * 25);
        int altura = alturaPainel * 3 + 35;
        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"").append(largura)
                .append("\" height=\"").append(altura).append("\" viewBox=\"0 0 ")
                .append(largura).append(' ').append(altura).append("\">\n")
                .append("<rect width=\"100%\" height=\"100%\" fill=\"#f8fafc\"/>\n")
                .append("<style>text{font-family:Segoe UI,Arial,sans-serif;fill:#172033}.title{font-size:22px;font-weight:700}.label{font-size:13px}.value{font-size:12px;fill:#334155}</style>\n")
                .append("<text x=\"30\" y=\"32\" class=\"title\">Histórico de desempenho — todas as execuções</text>\n");

        String[] metricas = {"tempo", "speedup", "eficiencia"};
        String[] titulos = {"Tempo de execução (ms)", "Speedup", "Eficiência (%)"};
        String[] cores = {"#2563eb", "#16a34a", "#ea580c"};
        int[] numeracaoExecucoes = numerarExecucoes(registros);
        for (int painel = 0; painel < metricas.length; painel++) {
            int topo = 54 + painel * alturaPainel;
            double maximo = valorMaximo(registros, metricas[painel]);
            svg.append("<text x=\"30\" y=\"").append(topo + 20).append("\" class=\"title\">")
                    .append(escaparXml(titulos[painel])).append("</text>\n");

            for (int i = 0; i < registros.size(); i++) {
                Registro registro = registros.get(i);
                int y = topo + 43 + i * 25;
                double valor = obterValor(registro, metricas[painel]);
                int barra = maximo == 0.0 ? 0 : (int) Math.round(valor / maximo * 610.0);
                String label = String.format(Locale.ROOT, "R%d | %s | %s | %d th",
                        numeracaoExecucoes[i], registro.analise, registro.metodo, registro.threads);
                svg.append("<text x=\"30\" y=\"").append(y + 14).append("\" class=\"label\">")
                        .append(escaparXml(label)).append("</text>\n")
                        .append("<rect x=\"475\" y=\"").append(y).append("\" width=\"").append(barra)
                        .append("\" height=\"17\" rx=\"3\" fill=\"").append(cores[painel]).append("\"/>\n")
                        .append("<text x=\"").append(485 + barra).append("\" y=\"").append(y + 13)
                        .append("\" class=\"value\">").append(String.format(Locale.ROOT, "%.3f", valor))
                        .append("</text>\n");
            }
        }
        svg.append("</svg>");
        Files.write(ARQUIVO_SVG, svg.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static int[] numerarExecucoes(List<Registro> registros) {
        int[] numeros = new int[registros.size()];
        Map<String, Integer> numeroPorExecucao = new HashMap<>();
        Map<String, Integer> proximoNumeroPorAnalise = new HashMap<>();
        for (int i = 0; i < registros.size(); i++) {
            Registro registro = registros.get(i);
            String chave = registro.analise + "|" + registro.execucao;
            Integer numero = numeroPorExecucao.get(chave);
            if (numero == null) {
                numero = proximoNumeroPorAnalise.getOrDefault(registro.analise, 0) + 1;
                numeroPorExecucao.put(chave, numero);
                proximoNumeroPorAnalise.put(registro.analise, numero);
            }
            numeros[i] = numero;
        }
        return numeros;
    }

    private static String escaparXml(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
