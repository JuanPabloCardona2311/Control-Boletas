package co.elmulardeoro.boletas.boleta;

import co.elmulardeoro.boletas.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.Instant;

@Entity
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venta_id")
    private Venta venta;

    private long valor;

    @Enumerated(EnumType.STRING)
    private MetodoPago metodo;

    @Enumerated(EnumType.STRING)
    private EstadoPago estado;

    private String nota;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrado_por")
    private Usuario registradoPor;

    @Column(name = "registrado_en")
    private Instant registradoEn = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revisado_por")
    private Usuario revisadoPor;

    @Column(name = "revisado_en")
    private Instant revisadoEn;

    protected Pago() {}

    public Pago(Venta venta, long valor, MetodoPago metodo, EstadoPago estado, String nota, Usuario registradoPor) {
        this.venta = venta;
        this.valor = valor;
        this.metodo = metodo;
        this.estado = estado;
        this.nota = nota;
        this.registradoPor = registradoPor;
    }

    public void revisar(EstadoPago nuevo, Usuario por) {
        this.estado = nuevo;
        this.revisadoPor = por;
        this.revisadoEn = Instant.now();
    }

    public Long getId() { return id; }
    public Venta getVenta() { return venta; }
    public long getValor() { return valor; }
    public MetodoPago getMetodo() { return metodo; }
    public EstadoPago getEstado() { return estado; }
    public String getNota() { return nota; }
    public Usuario getRegistradoPor() { return registradoPor; }
    public Instant getRegistradoEn() { return registradoEn; }
    public Usuario getRevisadoPor() { return revisadoPor; }
    public Instant getRevisadoEn() { return revisadoEn; }
}
