package pe.uch.intro_spring_mvc;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/** Atiende los errores que ocurren antes de llegar al controlador. */
@ControllerAdvice
public class ManejadorErrores {

    // Si la foto supera el limite del servidor, se vuelve al marcador con un aviso claro
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String fotoMuyPesada() {
        return "redirect:/marcador?error=foto";
    }
}
