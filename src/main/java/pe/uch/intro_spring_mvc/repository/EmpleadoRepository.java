package pe.uch.intro_spring_mvc.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.uch.intro_spring_mvc.model.Empleado;

public interface EmpleadoRepository extends JpaRepository<Empleado, String> {
}
