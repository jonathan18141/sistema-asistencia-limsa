package pe.uch.intro_spring_mvc.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.uch.intro_spring_mvc.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, String> {
}
