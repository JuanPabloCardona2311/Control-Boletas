package co.elmulardeoro.boletas.boleta;

import co.elmulardeoro.boletas.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Una boleta en manos de un comprador. Si se libera, la venta queda como historial con sus pagos. */
@Entity
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boleta_id")
    private Boleta boleta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comprador_id")
    private Comprador comprador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendedor_id")
    private Usuario vendedor;

    @Column(name = "creada_en")
    private Instant creadaEn = Instant.now();

    @Column(name = "liberada_en")
    private Instant liberadaEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "liberada_por")
    private Usuario liberadaPor;

    @Column(name = "motivo_liberacion")
    private String motivoLiberacion;

    @OneToMany(mappedBy = "venta")
    @OrderBy("registradoEn")
    private List<Pago> pagos = new ArrayList<>();

    protected Venta() {}

    public Venta(Boleta boleta, Comprador comprador, Usuario vendedor) {
        this.boleta = boleta;
        this.comprador = comprador;
        this.vendedor = vendedor;
    }

    public long totalConfirmado() {
        return total(EstadoPago.CONFIRMADO);
    }

    public long totalPorConfirmar() {
        return total(EstadoPago.POR_CONFIRMAR);
    }

    private long total(EstadoPago estado) {
        return pagos.stream().filter(p -> p.getEstado() == estado).mapToLong(Pago::getValor).sum();
    }

    public void liberar(Usuario por, String motivo) {
        this.liberadaEn = Instant.now();
        this.liberadaPor = por;
        this.motivoLiberacion = motivo;
    }

    public Long getId() { return id; }
    public Boleta getBoleta() { return boleta; }
    public Comprador getComprador() { return comprador; }
    public Usuario getVendedor() { return vendedor; }
    public Instant getCreadaEn() { return creadaEn; }
    public Instant getLiberadaEn() { return liberadaEn; }
    public Usuario getLiberadaPor() { return liberadaPor; }
    public String getMotivoLiberacion() { return motivoLiberacion; }
    public List<Pago> getPagos() { return pagos; }
}
