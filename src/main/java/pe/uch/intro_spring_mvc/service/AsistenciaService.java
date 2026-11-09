package pe.uch.intro_spring_mvc.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.imageio.ImageIO;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.uch.intro_spring_mvc.model.Empleado;
import pe.uch.intro_spring_mvc.model.FotoMarcacion;
import pe.uch.intro_spring_mvc.model.RegistroAsistencia;
import pe.uch.intro_spring_mvc.repository.EmpleadoRepository;
import pe.uch.intro_spring_mvc.repository.FotoMarcacionRepository;
import pe.uch.intro_spring_mvc.repository.RegistroAsistenciaRepository;

@Service
public class AsistenciaService {

    public static final ZoneId ZONA = ZoneId.of("America/Lima");
    public static final double JORNADA_HORAS = 8.0;
    /** Si el % de marcajes en planta supera este valor se sustenta un segundo huellero. */
    public static final double UMBRAL_PLANTA = 20.0;

    public static final int FOTO_MAX_BYTES = 8 * 1024 * 1024;   // 8 MB
    public static final int FOTO_MIN_LADO = 200;                // px: descarta imagenes diminutas

    public static final String EN_JORNADA = "EN_JORNADA";
    public static final String CONFORME = "CONFORME";
    public static final String OBSERVADO = "OBSERVADO_JORNADA_INCOMPLETA";
    public static final String PENDIENTE_PLANTA = "PENDIENTE_APROBACION_PLANTA";

    private final EmpleadoRepository empleadoRepository;
    private final RegistroAsistenciaRepository registroRepository;
    private final FotoMarcacionRepository fotoRepository;

    public AsistenciaService(EmpleadoRepository empleadoRepository,
                             RegistroAsistenciaRepository registroRepository,
                             FotoMarcacionRepository fotoRepository) {
        this.empleadoRepository = empleadoRepository;
        this.registroRepository = registroRepository;
        this.fotoRepository = fotoRepository;
    }

    /**
     * Marca asistencia. La foto es OBLIGATORIA tanto en la entrada como en la salida
     * (JPG o PNG, maximo 8 MB). El desafio indica que prueba de vida supero el trabajador en el navegador.
     */
    @Transactional
    public RegistroAsistencia marcar(String dniIngresado, byte[] foto, String desafio) {
        String dni = dniIngresado == null ? "" : dniIngresado.trim();

        // Validacion 1: formato
        if (!dni.matches("\\d{8}")) {
            throw new AsistenciaException("El DNI debe tener exactamente 8 dígitos numéricos.");
        }
        // Validacion 2: existencia
        Empleado empleado = empleadoRepository.findById(dni)
                .orElseThrow(() -> new AsistenciaException("El DNI " + dni + " no existe en el sistema."));

        // Validacion 3: la foto es obligatoria y debe ser una imagen real y decodificable
        if (foto == null || foto.length == 0) {
            throw new AsistenciaException("La foto es obligatoria: debes verificar tu rostro con la cámara para marcar.");
        }
        if (foto.length > FOTO_MAX_BYTES) {
            throw new AsistenciaException("La foto es muy pesada (máximo 8 MB).");
        }
        String tipoFoto = detectarTipoImagen(foto);
        if (tipoFoto == null) {
            throw new AsistenciaException("La foto debe ser una imagen JPG o PNG.");
        }
        validarDimensiones(foto);

        // Alternancia ENTRADA/SALIDA solo con las marcas DEL DIA (corrige el error del historial completo)
        LocalDateTime ahora = LocalDateTime.now(ZONA);
        LocalDate hoy = ahora.toLocalDate();
        List<RegistroAsistencia> delDia =
                registroRepository.marcasDelDia(dni, hoy.atStartOfDay(), hoy.plusDays(1).atStartOfDay());

        if (delDia.size() >= 2) {
            throw new AsistenciaException("Ya registraste tu entrada y salida de hoy.");
        }

        RegistroAsistencia registro = new RegistroAsistencia();
        registro.setEmpleado(empleado);
        registro.setFechaHora(ahora);
        registro.setUbicacion(empleado.getUbicacion());

        boolean esPlanta = Empleado.PLANTA.equals(empleado.getUbicacion());

        if (delDia.isEmpty()) {
            registro.setTipoMarca(RegistroAsistencia.ENTRADA);
            registro.setEstado(esPlanta ? PENDIENTE_PLANTA : EN_JORNADA);
        } else {
            RegistroAsistencia entrada = delDia.get(0);
            double horas = Duration.between(entrada.getFechaHora(), ahora).toMinutes() / 60.0;
            horas = Math.round(horas * 100.0) / 100.0;

            registro.setTipoMarca(RegistroAsistencia.SALIDA);
            registro.setHorasTrabajadas(horas);
            if (esPlanta) {
                registro.setEstado(PENDIENTE_PLANTA);              // RRHH debe aprobar
            } else {
                registro.setEstado(horas >= JORNADA_HORAS ? CONFORME : OBSERVADO);
            }
        }
        RegistroAsistencia guardado = registroRepository.save(registro);
        fotoRepository.save(new FotoMarcacion(guardado.getId(), tipoFoto, foto, ahora, desafio));
        return guardado;
    }

    /** Reconoce JPG o PNG por los primeros bytes del archivo (no por el nombre). */
    static String detectarTipoImagen(byte[] b) {
        if (b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (b.length > 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return "image/png";
        }
        return null;
    }

    /** Comprueba que los bytes se decodifiquen como imagen y que no sea diminuta. */
    static void validarDimensiones(byte[] foto) {
        BufferedImage img;
        try {
            img = ImageIO.read(new ByteArrayInputStream(foto));
        } catch (IOException e) {
            img = null;
        }
        if (img == null) {
            throw new AsistenciaException("La foto está dañada o no es una imagen válida.");
        }
        if (img.getWidth() < FOTO_MIN_LADO || img.getHeight() < FOTO_MIN_LADO) {
            throw new AsistenciaException("La foto es muy pequeña. Acércate a la cámara y vuelve a intentar.");
        }
    }

    @Transactional(readOnly = true)
    public Optional<FotoMarcacion> buscarFoto(Long marcacionId) {
        return marcacionId == null ? Optional.empty() : fotoRepository.findById(marcacionId);
    }

    /** Ids de las marcaciones que tienen foto (sin cargar las imagenes). */
    @Transactional(readOnly = true)
    public Set<Long> idsConFoto() {
        return new HashSet<>(fotoRepository.todosLosIds());
    }

    @Transactional(readOnly = true)
    public List<RegistroAsistencia> ultimasMarcaciones() {
        return registroRepository.findTop100ByOrderByFechaHoraDesc();
    }

    @Transactional(readOnly = true)
    public List<Empleado> listarEmpleados() {
        return empleadoRepository.findAll(Sort.by("nombre"));
    }

    /** Marcaciones filtradas por rango de fechas, ubicacion y estado (maximo 300 filas en pantalla). */
    @Transactional(readOnly = true)
    public List<RegistroAsistencia> filtrar(LocalDate desde, LocalDate hasta, String ubicacion, String estado) {
        return consultar(desde, hasta, ubicacion, estado).stream().limit(300).toList();
    }

    private List<RegistroAsistencia> consultar(LocalDate desde, LocalDate hasta, String ubicacion, String estado) {
        LocalDateTime d = (desde == null ? LocalDate.of(2000, 1, 1) : desde).atStartOfDay();
        LocalDateTime h = (hasta == null ? LocalDate.of(2100, 1, 1) : hasta).plusDays(1).atStartOfDay();
        String u = ubicacion == null ? "" : ubicacion.trim().toUpperCase();
        String e = estado == null ? "" : estado.trim().toUpperCase();
        return registroRepository.filtrar(d, h, u, e);
    }

    /** Excel (.xlsx) con todas las marcaciones que cumplen los filtros (sin limite de filas). */
    @Transactional(readOnly = true)
    public byte[] generarExcel(LocalDate desde, LocalDate hasta, String ubicacion, String estado) {
        DateTimeFormatter fecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter hora = DateTimeFormatter.ofPattern("HH:mm:ss");

        Set<Long> conFoto = idsConFoto();
        List<List<Object>> filas = new ArrayList<>();
        for (RegistroAsistencia r : consultar(desde, hasta, ubicacion, estado)) {
            List<Object> fila = new ArrayList<>();
            fila.add(r.getId());
            fila.add(r.getEmpleado().getDni());
            fila.add(r.getEmpleado().getNombre());
            fila.add(r.getEmpleado().getArea());
            fila.add(r.getEmpleado().getTurnoAsignado());
            fila.add(r.getFechaHora().format(fecha));
            fila.add(r.getFechaHora().format(hora));
            fila.add(r.getTipoMarca());
            fila.add(r.getUbicacion());
            fila.add(r.getHorasTrabajadas());
            fila.add(r.getEstado().replace('_', ' '));
            fila.add(conFoto.contains(r.getId()) ? "Sí" : "No");
            fila.add(r.getAprobadoPor());
            fila.add(r.getFechaAprobacion() == null ? null : r.getFechaAprobacion().format(fecha) + " " + r.getFechaAprobacion().format(hora));
            filas.add(fila);
        }
        return ExcelExportador.generar("Asistencias",
                List.of("N°", "DNI", "Empleado", "Área / Cargo", "Turno asignado", "Fecha", "Hora",
                        "Tipo de marca", "Ubicación", "Horas trabajadas", "Estado", "Foto de evidencia", "Aprobado por", "Fecha de aprobación"),
                filas);
    }

    /** PDF (A4 horizontal) con las marcaciones que cumplen los filtros. */
    @Transactional(readOnly = true)
    public byte[] generarPdf(LocalDate desde, LocalDate hasta, String ubicacion, String estado, String generadoPor) {
        DateTimeFormatter fecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter hora = DateTimeFormatter.ofPattern("HH:mm:ss");
        List<RegistroAsistencia> registros = consultar(desde, hasta, ubicacion, estado);
        Set<Long> conFoto = idsConFoto();

        List<List<String>> filas = new ArrayList<>();
        long pendientes = 0;
        long observadas = 0;
        for (RegistroAsistencia r : registros) {
            if (PENDIENTE_PLANTA.equals(r.getEstado())) {
                pendientes++;
            }
            if (OBSERVADO.equals(r.getEstado())) {
                observadas++;
            }
            filas.add(List.of(
                String.valueOf(r.getId()),
                r.getEmpleado().getDni(),
                r.getEmpleado().getNombre(),
                r.getFechaHora().format(fecha),
                r.getFechaHora().format(hora),
                r.getTipoMarca(),
                r.getHorasTrabajadas() == null ? "—" : r.getHorasTrabajadas() + " h",
                r.getEstado().replace('_', ' '),
                conFoto.contains(r.getId()) ? "Sí" : "—",
                r.getAprobadoPor() == null ? "" : r.getAprobadoPor()));
        }

        List<String> filtros = new ArrayList<>();
        if (desde != null) {
            filtros.add("desde " + desde.format(fecha));
        }
        if (hasta != null) {
            filtros.add("hasta " + hasta.format(fecha));
        }
        if (ubicacion != null && !ubicacion.isBlank()) {
            filtros.add("ubicación " + ubicacion);
        }
        if (estado != null && !estado.isBlank()) {
            filtros.add("estado " + estado.replace('_', ' '));
        }
        String lineaFiltros = filtros.isEmpty() ? "Filtros: sin filtros (todas las asistencias)"
                                                : "Filtros: " + String.join("  ·  ", filtros);
        String lineaGenerado = "Generado el " + LocalDateTime.now(ZONA).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                + (generadoPor == null || generadoPor.isBlank() ? "" : "  ·  por " + generadoPor);

        return PdfExportador.generar("Reporte de asistencia",
                "B&B LIMSA S.A.C.  ·  Recursos Humanos / Operaciones",
                List.of(lineaGenerado, lineaFiltros,
                        "Registros incluidos: " + registros.size() + "   ·   Pendientes de aprobación: " + pendientes
                                + "   ·   Jornadas observadas: " + observadas),
                List.of("N°", "DNI", "Empleado", "Fecha", "Hora", "Marca", "Horas", "Estado", "Foto", "Aprobado por"),
                new float[] {32, 62, 150, 60, 52, 60, 44, 180, 40, 80},
                filas);
    }

    /** Elimina un empleado solo si no tiene marcaciones registradas. */
    @Transactional
    public Empleado eliminarEmpleado(String dni) {
        Empleado e = empleadoRepository.findById(dni == null ? "" : dni.trim())
                .orElseThrow(() -> new AsistenciaException("El empleado no existe."));
        if (registroRepository.existsByEmpleadoDni(e.getDni())) {
            throw new AsistenciaException("No se puede eliminar a " + e.getNombre()
                    + ": ya tiene marcaciones registradas.");
        }
        empleadoRepository.delete(e);
        return e;
    }

    /** RRHH aprueba una marca de planta pendiente. */
    @Transactional
    public RegistroAsistencia aprobar(Long id, String aprobadoPor) {
        RegistroAsistencia r = registroRepository.findById(id)
                .orElseThrow(() -> new AsistenciaException("La marcación no existe."));
        if (!PENDIENTE_PLANTA.equals(r.getEstado())) {
            throw new AsistenciaException("Esta marcación no está pendiente de aprobación.");
        }
        if (RegistroAsistencia.SALIDA.equals(r.getTipoMarca()) && r.getHorasTrabajadas() != null) {
            r.setEstado(r.getHorasTrabajadas() >= JORNADA_HORAS ? CONFORME : OBSERVADO);
        } else {
            r.setEstado(CONFORME);
        }
        r.setFechaAprobacion(LocalDateTime.now(ZONA));
        r.setAprobadoPor(aprobadoPor);
        return registroRepository.save(r);
    }

    /** Registra un empleado nuevo con sus validaciones. */
    @Transactional
    public Empleado registrarEmpleado(String dni, String nombre, String area, String turno, String ubicacion) {
        String d = dni == null ? "" : dni.trim();
        String n = nombre == null ? "" : nombre.trim();
        String a = area == null ? "" : area.trim();
        String t = turno == null ? "" : turno.trim();
        String u = ubicacion == null ? "" : ubicacion.trim().toUpperCase();

        if (!d.matches("\\d{8}")) {
            throw new AsistenciaException("El DNI debe tener exactamente 8 dígitos numéricos.");
        }
        if (n.isEmpty() || a.isEmpty() || t.isEmpty()) {
            throw new AsistenciaException("Nombre, área y turno son obligatorios.");
        }
        if (n.length() > 100 || a.length() > 100 || t.length() > 40) {
            throw new AsistenciaException("Algún campo supera la longitud permitida.");
        }
        if (!Empleado.PLANTA.equals(u) && !Empleado.OFICINA.equals(u)) {
            throw new AsistenciaException("La ubicación debe ser PLANTA u OFICINA.");
        }
        if (empleadoRepository.existsById(d)) {
            throw new AsistenciaException("El DNI " + d + " ya está registrado.");
        }
        return empleadoRepository.save(new Empleado(d, n, a, t, u));
    }

    @Transactional(readOnly = true)
    public long totalPendientes() {
        return registroRepository.countByEstado(PENDIENTE_PLANTA);
    }

    @Transactional(readOnly = true)
    public long totalMarcaciones() {
        return registroRepository.count();
    }

    @Transactional(readOnly = true)
    public long totalObservadas() {
        return registroRepository.countByEstado(OBSERVADO);
    }

    /** Indicador: % de marcajes realizados por personal de planta sobre el total. */
    @Transactional(readOnly = true)
    public double porcentajePlanta() {
        long total = registroRepository.count();
        if (total == 0) {
            return 0.0;
        }
        return registroRepository.countByUbicacion(Empleado.PLANTA) * 100.0 / total;
    }
}
