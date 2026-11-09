package pe.uch.intro_spring_mvc.service;

/** Error de negocio o de validacion al marcar asistencia (el mensaje se muestra al usuario). */
public class AsistenciaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AsistenciaException(String mensaje) {
        super(mensaje);
    }
}
