package pe.uch.intro_spring_mvc.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/** Foto de evidencia que el trabajador sube al marcar. Se guarda en MySQL, aparte de la marcación. */
@Entity
@Table(name = "fotos_marcacion")
public class FotoMarcacion {

    @Id
    @Column(name = "marcacion_id")
    private Long marcacionId;

    @Column(name = "tipo_contenido", length = 30, nullable = false)
    private String tipoContenido;          // image/jpeg o image/png

    @Lob
    @Column(name = "datos", nullable = false, length = 16777215)   // MEDIUMBLOB o mayor: la foto pesa cientos de KB
    private byte[] datos;

    @Column(name = "fecha_subida", nullable = false)
    private LocalDateTime fechaSubida;

    /** Método de verificación usado (FOTO_ADJUNTA). */
    @Column(name = "desafio", length = 30)
    private String desafio;

    public FotoMarcacion() {}

    public FotoMarcacion(Long marcacionId, String tipoContenido, byte[] datos, LocalDateTime fechaSubida, String desafio) {
        this.marcacionId = marcacionId;
        this.tipoContenido = tipoContenido;
        this.datos = datos;
        this.fechaSubida = fechaSubida;
        this.desafio = desafio;
    }

    public Long getMarcacionId() { return marcacionId; }
    public void setMarcacionId(Long marcacionId) { this.marcacionId = marcacionId; }
    public String getTipoContenido() { return tipoContenido; }
    public void setTipoContenido(String tipoContenido) { this.tipoContenido = tipoContenido; }
    public byte[] getDatos() { return datos; }
    public void setDatos(byte[] datos) { this.datos = datos; }
    public LocalDateTime getFechaSubida() { return fechaSubida; }
    public void setFechaSubida(LocalDateTime fechaSubida) { this.fechaSubida = fechaSubida; }
    public String getDesafio() { return desafio; }
    public void setDesafio(String desafio) { this.desafio = desafio; }
}
