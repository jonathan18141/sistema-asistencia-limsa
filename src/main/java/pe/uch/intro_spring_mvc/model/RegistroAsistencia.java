package pe.uch.intro_spring_mvc.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "marcaciones",
       indexes = @Index(name = "idx_marcaciones_dni_fecha", columnList = "dni, fecha_hora"))
public class RegistroAsistencia {

    public static final String ENTRADA = "ENTRADA";
    public static final String SALIDA = "SALIDA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "dni", nullable = false)
    private Empleado empleado;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "tipo_marca", length = 10, nullable = false)
    private String tipoMarca; // ENTRADA o SALIDA

    // Copia de la ubicacion del empleado al momento de marcar (para el indicador de planta)
    @Column(name = "ubicacion", length = 10, nullable = false)
    private String ubicacion;

    // Solo se llena en la marca de SALIDA
    @Column(name = "horas_trabajadas")
    private Double horasTrabajadas;

    // EN_JORNADA, CONFORME, OBSERVADO_JORNADA_INCOMPLETA, PENDIENTE_APROBACION_PLANTA
    @Column(name = "estado", length = 40, nullable = false)
    private String estado;

    // Se llena cuando RRHH aprueba una marca de planta
    @Column(name = "fecha_aprobacion")
    private LocalDateTime fechaAprobacion;

    // Usuario de RRHH que aprobo la marca
    @Column(name = "aprobado_por", length = 30)
    private String aprobadoPor;

    public RegistroAsistencia() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Empleado getEmpleado() { return empleado; }
    public void setEmpleado(Empleado empleado) { this.empleado = empleado; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }
    public String getTipoMarca() { return tipoMarca; }
    public void setTipoMarca(String tipoMarca) { this.tipoMarca = tipoMarca; }
    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }
    public Double getHorasTrabajadas() { return horasTrabajadas; }
    public void setHorasTrabajadas(Double horasTrabajadas) { this.horasTrabajadas = horasTrabajadas; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public LocalDateTime getFechaAprobacion() { return fechaAprobacion; }
    public void setFechaAprobacion(LocalDateTime fechaAprobacion) { this.fechaAprobacion = fechaAprobacion; }
    public String getAprobadoPor() { return aprobadoPor; }
    public void setAprobadoPor(String aprobadoPor) { this.aprobadoPor = aprobadoPor; }
}
