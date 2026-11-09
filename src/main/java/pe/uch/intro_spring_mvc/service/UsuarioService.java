package pe.uch.intro_spring_mvc.service;

import java.util.Optional;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.uch.intro_spring_mvc.model.Usuario;
import pe.uch.intro_spring_mvc.repository.UsuarioRepository;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder encoder;

    public UsuarioService(UsuarioRepository usuarioRepository, BCryptPasswordEncoder encoder) {
        this.usuarioRepository = usuarioRepository;
        this.encoder = encoder;
    }

    /** Devuelve el usuario si el nombre y la clave son correctos; vacio en caso contrario. */
    @Transactional(readOnly = true)
    public Optional<Usuario> autenticar(String usuario, String clave) {
        if (usuario == null || clave == null || usuario.isBlank()) {
            return Optional.empty();
        }
        return usuarioRepository.findById(usuario.trim().toLowerCase())
                .filter(u -> encoder.matches(clave, u.getClaveHash()));
    }
}
