package co.elmulardeoro.boletas.boleta;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;

@Entity
public class Comprador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    private String celular;

    private String ubicacion;

    @Column(name = "creado_en", insertable = false, updatable = false)
    private Instant creadoEn;

    protected Comprador() {}

    public Comprador(String nombre, String celular, String ubicacion) {
        this.nombre = nombre;
        this.celular = celular;
        this.ubicacion = ubicacion;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCelular() { return celular; }
    public void setCelular(String celular) { this.celular = celular; }
    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }
}
