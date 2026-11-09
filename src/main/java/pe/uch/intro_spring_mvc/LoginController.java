package pe.uch.intro_spring_mvc;

import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import pe.uch.intro_spring_mvc.model.Usuario;
import pe.uch.intro_spring_mvc.service.UsuarioService;

@Controller
public class LoginController {

    private final UsuarioService usuarioService;

    public LoginController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/login")
    public String mostrarLogin(HttpSession session) {
        if (session.getAttribute("usuario") != null) {
            return "redirect:/reporte";
        }
        return "login";
    }

    @PostMapping("/login")
    public String iniciarSesion(@RequestParam("usuario") String usuario,
                                @RequestParam("clave") String clave,
                                HttpServletRequest request,
                                Model model) {
        Optional<Usuario> encontrado = usuarioService.autenticar(usuario, clave);
        if (encontrado.isEmpty()) {
            model.addAttribute("error", "Usuario o contraseña incorrectos.");
            return "login";
        }
        HttpSession session = request.getSession(true);
        request.changeSessionId();   // evita la fijacion de sesion
        Usuario u = encontrado.get();
        session.setAttribute("usuario", u.getUsuario());
        session.setAttribute("nombreUsuario", u.getNombre());
        session.setAttribute("rolUsuario", u.getRol());
        return "redirect:/reporte";
    }

    @PostMapping("/logout")
    public String cerrarSesion(HttpSession session) {
        session.invalidate();
        return "redirect:/marcador";
    }
}
