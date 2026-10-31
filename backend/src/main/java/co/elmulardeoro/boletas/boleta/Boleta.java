package co.elmulardeoro.boletas.boleta;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import java.time.Instant;

@Entity
public class Boleta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rifa_id")
    private Long rifaId;

    private int numero;

    /** La venta vigente; null cuando el número está libre. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venta_id")
    private Venta venta;

    @Column(name = "actualizada_en")
    private Instant actualizadaEn;

    protected Boleta() {}

    public void asignar(Venta venta) {
        this.venta = venta;
        tocar();
    }

    public void tocar() {
        this.actualizadaEn = Instant.now();
    }

    public Long getId() { return id; }
    public Long getRifaId() { return rifaId; }
    public int getNumero() { return numero; }
    public Venta getVenta() { return venta; }
}
