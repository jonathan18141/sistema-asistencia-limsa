package pe.uch.intro_spring_mvc.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pe.uch.intro_spring_mvc.model.FotoMarcacion;

public interface FotoMarcacionRepository extends JpaRepository<FotoMarcacion, Long> {

    // Solo los ids (sin cargar las imagenes) para saber que marcaciones tienen foto
    @Query("select f.marcacionId from FotoMarcacion f")
    List<Long> todosLosIds();
}
