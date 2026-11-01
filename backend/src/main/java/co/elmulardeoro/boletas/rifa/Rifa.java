package co.elmulardeoro.boletas.rifa;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Rifa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    @Column(name = "cantidad_numeros")
    private int cantidadNumeros;

    private long precio;

    private String loteria;

    @Column(name = "fecha_sorteo")
    private LocalDate fechaSorteo;

    @Column(name = "dias_recordatorio")
    private int diasRecordatorio;

    @Column(name = "dias_liberacion")
    private int diasLiberacion;

    private String responsables;

    private boolean activa = true;

    private String resultado;

    @Column(name = "creada_en", insertable = false, updatable = false)
    private Instant creadaEn;

    @OneToMany(mappedBy = "rifa", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden")
    private List<Premio> premios = new ArrayList<>();

    protected Rifa() {}

    public Rifa(String nombre, int cantidadNumeros, long precio, String loteria, LocalDate fechaSorteo) {
        this.nombre = nombre;
        this.cantidadNumeros = cantidadNumeros;
        this.precio = precio;
        this.loteria = loteria;
        this.fechaSorteo = fechaSorteo;
    }

    /** Cantidad de cifras de cada boleta: 3 para 000-999, 4 para 0000-9999. */
    public int cifras() {
        return String.valueOf(cantidadNumeros - 1).length();
    }

    public String formatear(int numero) {
        return String.format("%0" + cifras() + "d", numero);
    }

    public LocalDate fechaRecordatorio() {
        return fechaSorteo.minusDays(diasRecordatorio);
    }

    public LocalDate fechaLiberacion() {
        return fechaSorteo.minusDays(diasLiberacion);
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public int getCantidadNumeros() { return cantidadNumeros; }
    public long getPrecio() { return precio; }
    public void setPrecio(long precio) { this.precio = precio; }
    public String getLoteria() { return loteria; }
    public void setLoteria(String loteria) { this.loteria = loteria; }
    public LocalDate getFechaSorteo() { return fechaSorteo; }
    public void setFechaSorteo(LocalDate fechaSorteo) { this.fechaSorteo = fechaSorteo; }
    public int getDiasRecordatorio() { return diasRecordatorio; }
    public void setDiasRecordatorio(int diasRecordatorio) { this.diasRecordatorio = diasRecordatorio; }
    public int getDiasLiberacion() { return diasLiberacion; }
    public void setDiasLiberacion(int diasLiberacion) { this.diasLiberacion = diasLiberacion; }
    public String getResponsables() { return responsables; }
    public void setResponsables(String responsables) { this.responsables = responsables; }
    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }
    public String getResultado() { return resultado; }
    public void setResultado(String resultado) { this.resultado = resultado; }
    public Instant getCreadaEn() { return creadaEn; }
    public List<Premio> getPremios() { return premios; }
}
