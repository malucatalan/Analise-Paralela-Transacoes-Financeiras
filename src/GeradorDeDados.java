import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GeradorDeDados {
    private Random random = new Random();
    private String[] categorias = {"Alimentação", "Transporte", "Eletrônicos", "Saúde", "Educação"};
    private String[] tipos = {"Crédito", "Débito", "Pix"};

    public List<Transacao> gerarTransacoes(int quantidade) {
        List<Transacao> lista = new ArrayList<>(quantidade);
        
        for (int i = 0; i < quantidade; i++) {
            long id = 10000 + i;
            
            String cliente = gerarClienteUniforme(100000); 
            
            String data = "2026-10-12";
            String categoria = gerarCategoriaPonderada(); 
            double valor = gerarValorPorCategoria(categoria); 
            String cidade = "Sao Jose dos Campos";
            String estado = "SP";
            String tipo = tipos[random.nextInt(tipos.length)];

            lista.add(new Transacao(id, cliente, data, valor, categoria, cidade, estado, tipo));
        }
        return lista;
    }

    private String gerarClienteUniforme(int totalClientes) {
        return "C" + String.format("%06d", (random.nextInt(totalClientes) + 1));
    }

    private String gerarCategoriaPonderada() {
        double p = random.nextDouble();
        if (p < 0.40) return categorias[0]; //40% Alimentação
        if (p < 0.70) return categorias[1]; //30% Transporte
        if (p < 0.85) return categorias[2]; //15% Eletrônicos
        if (p < 0.95) return categorias[3]; //10% Saúde
        return categorias[4]; //5% Educação
    }

    private double gerarValorPorCategoria(String categoria) {
        switch (categoria) {
            case "Eletrônicos":
                //Normal
                double valorEletro = random.nextGaussian() * 1000 + 2500; 
                return Math.max(50.0, valorEletro);

            case "Educação":
                //Normal
                double valorEdu = random.nextGaussian() * 400 + 800;
                return Math.max(100.0, valorEdu);

            case "Alimentação":
                //Exponencial
                return -45.0 * Math.log(1 - random.nextDouble()) + 5.0;

            case "Transporte":
                //Exponencial
                return -30.0 * Math.log(1 - random.nextDouble()) + 8.0;

            case "Saúde":
                //Exponencial
                return -100.0 * Math.log(1 - random.nextDouble()) + 10.0;

            default:
                return 50.0;
        }
    }
}