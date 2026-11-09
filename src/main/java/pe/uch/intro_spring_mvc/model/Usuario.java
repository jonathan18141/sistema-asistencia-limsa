package pe.uch.intro_spring_mvc.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuarios")
public class Usuario {

    public static final String RRHH = "RRHH";         // aprueba marcas y registra empleados
    public static final String GERENCIA = "GERENCIA"; // solo consulta reportes

    @Id
    @Column(name = "usuario", length = 30, nullable = false)
    private String usuario;

    @Column(name = "clave_hash", length = 100, nullable = false)
    private String claveHash;

    @Column(name = "nombre", length = 100, nullable = false)
    private String nombre;

    @Column(name = "rol", length = 15, nullable = false)
    private String rol;

    public Usuario() {}

    public Usuario(String usuario, String claveHash, String nombre, String rol) {
        this.usuario = usuario;
        this.claveHash = claveHash;
        this.nombre = nombre;
        this.rol = rol;
    }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getClaveHash() { return claveHash; }
    public void setClaveHash(String claveHash) { this.claveHash = claveHash; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
}
