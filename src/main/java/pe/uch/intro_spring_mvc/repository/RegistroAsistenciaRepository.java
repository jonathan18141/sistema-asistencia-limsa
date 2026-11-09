package pe.uch.intro_spring_mvc.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.uch.intro_spring_mvc.model.RegistroAsistencia;

public interface RegistroAsistenciaRepository extends JpaRepository<RegistroAsistencia, Long> {

    // Marcas de un DNI dentro de un dia: [desde, hasta)
    @Query("select r from RegistroAsistencia r "
         + "where r.empleado.dni = :dni and r.fechaHora >= :desde and r.fechaHora < :hasta "
         + "order by r.fechaHora asc")
    List<RegistroAsistencia> marcasDelDia(@Param("dni") String dni,
                                          @Param("desde") LocalDateTime desde,
                                          @Param("hasta") LocalDateTime hasta);

    // Filtros del reporte: cadena vacia en ubicacion/estado = "todos"
    @Query("select r from RegistroAsistencia r "
         + "where r.fechaHora >= :desde and r.fechaHora < :hasta "
         + "and (:ubicacion = '' or r.ubicacion = :ubicacion) "
         + "and (:estado = '' or r.estado = :estado) "
         + "order by r.fechaHora desc")
    List<RegistroAsistencia> filtrar(@Param("desde") LocalDateTime desde,
                                     @Param("hasta") LocalDateTime hasta,
                                     @Param("ubicacion") String ubicacion,
                                     @Param("estado") String estado);

    List<RegistroAsistencia> findTop100ByOrderByFechaHoraDesc();

    long countByUbicacion(String ubicacion);

    long countByEstado(String estado);

    boolean existsByEmpleadoDni(String dni);
}
