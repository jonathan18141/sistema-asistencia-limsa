package pe.uch.intro_spring_mvc.config;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import pe.uch.intro_spring_mvc.model.Empleado;
import pe.uch.intro_spring_mvc.model.Usuario;
import pe.uch.intro_spring_mvc.repository.EmpleadoRepository;
import pe.uch.intro_spring_mvc.repository.UsuarioRepository;

/** Carga el personal de ejemplo solo si la tabla empleados esta vacia. */
@Component
public class CargaInicialDatos implements CommandLineRunner {

    private final EmpleadoRepository empleadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder encoder;

    public CargaInicialDatos(EmpleadoRepository empleadoRepository,
                             UsuarioRepository usuarioRepository,
                             BCryptPasswordEncoder encoder) {
        this.empleadoRepository = empleadoRepository;
        this.usuarioRepository = usuarioRepository;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (empleadoRepository.count() == 0) {
            empleadoRepository.saveAll(List.of(
                new Empleado("71234567", "Juan Pérez", "Lab. Metrología / Técnico", "08:00 AM - 05:00 PM", Empleado.PLANTA),
                new Empleado("73456789", "María Torres", "Control de Calidad (QA)", "08:30 AM - 05:30 PM", Empleado.PLANTA)
            ));
        }

        // Usuarios de ejemplo (la clave se guarda cifrada con BCrypt)
        if (usuarioRepository.count() == 0) {
            usuarioRepository.saveAll(List.of(
                new Usuario("rrhh", encoder.encode("rrhh2026"), "Recursos Humanos", Usuario.RRHH),
                new Usuario("gerencia", encoder.encode("gerencia2026"), "Gerente de Laboratorio", Usuario.GERENCIA)
            ));
        }
    }
}
