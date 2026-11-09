package pe.uch.intro_spring_mvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import pe.uch.intro_spring_mvc.model.Usuario;
import pe.uch.intro_spring_mvc.service.UsuarioService;

@SpringBootTest
class UsuarioServiceTest {

    @Autowired
    private UsuarioService service;

    @Test
    void credencialesCorrectasDevuelvenElUsuarioConSuRol() {
        Usuario u = service.autenticar("rrhh", "rrhh2026").orElseThrow();
        assertEquals(Usuario.RRHH, u.getRol());
        assertEquals(Usuario.GERENCIA, service.autenticar("GERENCIA", "gerencia2026").orElseThrow().getRol());
    }

    @Test
    void claveIncorrectaOUsuarioInexistenteSeRechaza() {
        assertTrue(service.autenticar("rrhh", "incorrecta").isEmpty());
        assertTrue(service.autenticar("nadie", "rrhh2026").isEmpty());
        assertTrue(service.autenticar("", "").isEmpty());
        assertTrue(service.autenticar(null, null).isEmpty());
    }
}
