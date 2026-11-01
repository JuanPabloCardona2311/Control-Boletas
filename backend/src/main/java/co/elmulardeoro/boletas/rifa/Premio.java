package co.elmulardeoro.boletas.rifa;

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

@Entity
public class Premio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rifa_id")
    private Rifa rifa;

    private String descripcion;

    @Enumerated(EnumType.STRING)
    private PosicionPremio posicion;

    private int cifras;

    @Column(name = "valor_efectivo")
    private Long valorEfectivo;

    private int orden;

    protected Premio() {}

    public Premio(Rifa rifa, String descripcion, PosicionPremio posicion, int cifras, Long valorEfectivo, int orden) {
        this.rifa = rifa;
        this.descripcion = descripcion;
        this.posicion = posicion;
        this.cifras = cifras;
        this.valorEfectivo = valorEfectivo;
        this.orden = orden;
    }

    /** Las cifras del resultado de la lotería que ganan este premio, p. ej. "234" para las 3 últimas de "1234". */
    public String cifrasGanadoras(String resultado) {
        return posicion == PosicionPremio.PRIMERAS
                ? resultado.substring(0, cifras)
                : resultado.substring(resultado.length() - cifras);
    }

    public Long getId() { return id; }
    public String getDescripcion() { return descripcion; }
    public PosicionPremio getPosicion() { return posicion; }
    public int getCifras() { return cifras; }
    public Long getValorEfectivo() { return valorEfectivo; }
    public int getOrden() { return orden; }
}
