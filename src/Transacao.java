public class Transacao {
    public long id;
    public String cliente;
    public String data;
    public double valor;
    public String categoria;
    public String cidade;
    public String estado;
    public String tipo;

    // Construtor
    public Transacao(long id, String cliente, String data, double valor, String categoria, String cidade, String estado, String tipo) {
        this.id = id;
        this.cliente = cliente;
        this.data = data;
        this.valor = valor;
        this.categoria = categoria;
        this.cidade = cidade;
        this.estado = estado;
        this.tipo = tipo;
    }

    @Override
    public String toString() {
        return "ID: " + id + " | Cliente: " + cliente + " | Categoria: " + categoria + " | Valor: R$ " + String.format("%.2f", valor);
    }
}