package pe.uch.intro_spring_mvc.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "empleados")
public class Empleado {

    public static final String OFICINA = "OFICINA";
    public static final String PLANTA = "PLANTA";

    @Id
    @Column(name = "dni", length = 8, nullable = false)
    private String dni;

    @Column(name = "nombre", length = 100, nullable = false)
    private String nombre;

    @Column(name = "area", length = 100, nullable = false)
    private String area;

    @Column(name = "turno_asignado", length = 40, nullable = false)
    private String turnoAsignado;

    // OFICINA (usa huellero) o PLANTA (marca por la web)
    @Column(name = "ubicacion", length = 10, nullable = false)
    private String ubicacion;

    public Empleado() {} // JPA lo necesita

    public Empleado(String dni, String nombre, String area, String turnoAsignado, String ubicacion) {
        this.dni = dni;
        this.nombre = nombre;
        this.area = area;
        this.turnoAsignado = turnoAsignado;
        this.ubicacion = ubicacion;
    }

    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }
    public String getTurnoAsignado() { return turnoAsignado; }
    public void setTurnoAsignado(String turnoAsignado) { this.turnoAsignado = turnoAsignado; }
    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }
}
