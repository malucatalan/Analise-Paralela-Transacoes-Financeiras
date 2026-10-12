import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("Iniciando a geração de dados...");
        
        GeradorDeDados gerador = new GeradorDeDados();
        
        int quantidade = 10; 
        List<Transacao> meusDados = gerador.gerarTransacoes(quantidade);

        System.out.println("Foram geradas " + meusDados.size() + " transações com sucesso!\n");

        for (Transacao t : meusDados) {
            System.out.println(t.toString());
        }
    }
}