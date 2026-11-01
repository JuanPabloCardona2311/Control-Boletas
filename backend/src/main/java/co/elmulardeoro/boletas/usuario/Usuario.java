package co.elmulardeoro.boletas.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;

@Entity
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    private String usuario;

    @Column(name = "clave_hash")
    private String claveHash;

    @Enumerated(EnumType.STRING)
    private Rol rol;

    private String celular;

    private boolean activo = true;

    @Column(name = "creado_en", insertable = false, updatable = false)
    private Instant creadoEn;

    protected Usuario() {}

    public Usuario(String nombre, String usuario, String claveHash, Rol rol, String celular) {
        this.nombre = nombre;
        this.usuario = usuario;
        this.claveHash = claveHash;
        this.rol = rol;
        this.celular = celular;
    }

    public boolean esAdmin() {
        return rol == Rol.ADMIN;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getUsuario() { return usuario; }
    public String getClaveHash() { return claveHash; }
    public void setClaveHash(String claveHash) { this.claveHash = claveHash; }
    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }
    public String getCelular() { return celular; }
    public void setCelular(String celular) { this.celular = celular; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public Instant getCreadoEn() { return creadoEn; }
}
