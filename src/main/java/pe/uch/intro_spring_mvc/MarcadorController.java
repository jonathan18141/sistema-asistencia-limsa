package pe.uch.intro_spring_mvc;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.security.SecureRandom;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.servlet.http.HttpSession;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import pe.uch.intro_spring_mvc.model.Empleado;
import pe.uch.intro_spring_mvc.model.FotoMarcacion;
import pe.uch.intro_spring_mvc.model.RegistroAsistencia;
import pe.uch.intro_spring_mvc.service.AsistenciaException;
import pe.uch.intro_spring_mvc.service.AsistenciaService;

@Controller
public class MarcadorController {

    /** Método de verificación: foto adjunta con rostro detectado en el navegador. */
    private static final List<String> DESAFIOS = List.of("FOTO_ADJUNTA");
    private static final long DESAFIO_VIGENCIA_MS = 5 * 60 * 1000L;   // 5 minutos
    private static final SecureRandom AZAR = new SecureRandom();

    private final AsistenciaService asistenciaService;

    public MarcadorController(AsistenciaService asistenciaService) {
        this.asistenciaService = asistenciaService;
    }

    @GetMapping("/")
    public String inicio() {
        return "redirect:/marcador";
    }

    // ---------- Marcador ----------

    @GetMapping("/marcador")
    public String mostrarMarcador(@RequestParam(name = "error", required = false) String codigo,
                                  HttpSession session, Model model) {
        // Desafío nuevo y de un solo uso por cada marcación (entrada o salida)
        String desafio = DESAFIOS.get(AZAR.nextInt(DESAFIOS.size()));
        String nonce = UUID.randomUUID().toString();
        session.setAttribute("desafioActual", desafio);
        session.setAttribute("desafioNonce", nonce);
        session.setAttribute("desafioExpira", System.currentTimeMillis() + DESAFIO_VIGENCIA_MS);
        model.addAttribute("desafio", desafio);
        model.addAttribute("nonce", nonce);

        List<RegistroAsistencia> recientes =
                asistenciaService.ultimasMarcaciones().stream().limit(6).toList();
        model.addAttribute("recientes", recientes);
        model.addAttribute("conFoto", asistenciaService.idsConFoto());
        if ("foto".equals(codigo)) {
            model.addAttribute("error", "La foto es muy pesada (máximo 8 MB). Intenta con otra.");
        }
        return "marcador-empleado";
    }

    @PostMapping("/marcador/marcar")
    public String marcarAsistencia(@RequestParam("dni") String dni,
                                   @RequestParam(name = "foto", required = false) MultipartFile foto,
                                   @RequestParam(name = "nonce", required = false) String nonce,
                                   HttpSession session,
                                   RedirectAttributes redirect) {
        try {
            // El desafío es de un solo uso: se consume aunque la marcación falle
            String desafio = consumirDesafio(session, nonce);
            byte[] bytesFoto = (foto == null || foto.isEmpty()) ? null : foto.getBytes();
            RegistroAsistencia r = asistenciaService.marcar(dni, bytesFoto, desafio);
            redirect.addFlashAttribute("mensaje",
                    "Marcación registrada: " + r.getEmpleado().getNombre() + " [" + r.getTipoMarca() + "]"
                            + " · foto verificada");
        } catch (AsistenciaException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        } catch (IOException e) {
            redirect.addFlashAttribute("error", "No se pudo leer la foto. Intenta de nuevo.");
        }
        return "redirect:/marcador";
    }

    /** Valida que el nonce enviado sea el que se entregó a esta sesión y que no haya vencido. */
    private String consumirDesafio(HttpSession session, String nonce) {
        Object esperado = session.getAttribute("desafioNonce");
        Object desafio = session.getAttribute("desafioActual");
        Object expira = session.getAttribute("desafioExpira");
        session.removeAttribute("desafioNonce");
        session.removeAttribute("desafioActual");
        session.removeAttribute("desafioExpira");

        if (esperado == null || nonce == null || !esperado.equals(nonce) || desafio == null) {
            throw new AsistenciaException("La verificación facial no es válida. Vuelve a intentarlo con la cámara.");
        }
        if (expira == null || System.currentTimeMillis() > (Long) expira) {
            throw new AsistenciaException("La verificación facial venció. Vuelve a intentarlo.");
        }
        return (String) desafio;
    }

    // ---------- Reporte (RRHH) ----------

    @GetMapping("/reporte")
    public String reporteAsistencias(
            @RequestParam(name = "desde", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(name = "hasta", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(name = "ubicacion", defaultValue = "") String ubicacion,
            @RequestParam(name = "estado", defaultValue = "") String estado,
            HttpSession session,
            Model model) {

        List<Empleado> empleados = asistenciaService.listarEmpleados();

        List<RegistroAsistencia> marcaciones = asistenciaService.filtrar(desde, hasta, ubicacion, estado);
        Set<Long> conFoto = asistenciaService.idsConFoto();

        model.addAttribute("conFoto", conFoto);
        model.addAttribute("nombreUsuario", session.getAttribute("nombreUsuario"));
        model.addAttribute("rolUsuario", session.getAttribute("rolUsuario"));
        model.addAttribute("empresa", "B&B LIMSA S.A.C.");
        model.addAttribute("area", "Recursos Humanos / Operaciones");
        model.addAttribute("marcaciones", marcaciones);
        model.addAttribute("empleados", empleados);
        model.addAttribute("totalEmpleados", empleados.size());
        model.addAttribute("totalMarcaciones", asistenciaService.totalMarcaciones());
        model.addAttribute("totalObservadas", asistenciaService.totalObservadas());
        model.addAttribute("totalPendientes", asistenciaService.totalPendientes());
        // Para conservar los filtros en el formulario
        model.addAttribute("filtroDesde", desde);
        model.addAttribute("filtroHasta", hasta);
        model.addAttribute("filtroUbicacion", ubicacion);
        model.addAttribute("filtroEstado", estado);
        return "asistencias-admin";
    }

    @PostMapping("/reporte/aprobar/{id}")
    public String aprobarMarca(@PathVariable("id") Long id, HttpSession session, RedirectAttributes redirect) {
        try {
            RegistroAsistencia r = asistenciaService.aprobar(id, (String) session.getAttribute("usuario"));
            redirect.addFlashAttribute("mensaje",
                    "Marcación aprobada: " + r.getEmpleado().getNombre() + " → " + r.getEstado());
        } catch (AsistenciaException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/reporte";
    }

    @PostMapping("/empleados/registrar")
    public String registrarEmpleado(@RequestParam("dni") String dni,
                                    @RequestParam("nombre") String nombre,
                                    @RequestParam("area") String area,
                                    @RequestParam("turno") String turno,
                                    @RequestParam("ubicacion") String ubicacion,
                                    RedirectAttributes redirect) {
        try {
            Empleado e = asistenciaService.registrarEmpleado(dni, nombre, area, turno, ubicacion);
            redirect.addFlashAttribute("mensaje", "Empleado registrado: " + e.getNombre() + " (" + e.getDni() + ")");
        } catch (AsistenciaException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/reporte";
    }

    /** Descarga un Excel con las asistencias (respeta los filtros del reporte). */
    @GetMapping("/reporte/exportar")
    public ResponseEntity<byte[]> exportarExcel(
            @RequestParam(name = "desde", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(name = "hasta", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(name = "ubicacion", defaultValue = "") String ubicacion,
            @RequestParam(name = "estado", defaultValue = "") String estado) {

        byte[] excel = asistenciaService.generarExcel(desde, hasta, ubicacion, estado);
        String nombre = "asistencias_" + LocalDate.now(AsistenciaService.ZONA)
                .format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }

    @PostMapping("/empleados/eliminar/{dni}")
    public String eliminarEmpleado(@PathVariable("dni") String dni, RedirectAttributes redirect) {
        try {
            Empleado e = asistenciaService.eliminarEmpleado(dni);
            redirect.addFlashAttribute("mensaje", "Empleado eliminado: " + e.getNombre());
        } catch (AsistenciaException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/reporte";
    }

    /** Descarga un PDF con las asistencias (respeta los filtros). Lo pueden usar RRHH y Gerencia. */
    @GetMapping("/reporte/exportar-pdf")
    public ResponseEntity<byte[]> exportarPdf(
            @RequestParam(name = "desde", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(name = "hasta", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(name = "ubicacion", defaultValue = "") String ubicacion,
            @RequestParam(name = "estado", defaultValue = "") String estado,
            HttpSession session) {

        String generadoPor = session.getAttribute("nombreUsuario") + " (" + session.getAttribute("rolUsuario") + ")";
        byte[] pdf = asistenciaService.generarPdf(desde, hasta, ubicacion, estado, generadoPor);
        String nombre = "asistencias_" + LocalDate.now(AsistenciaService.ZONA)
                .format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    /** Muestra la foto de evidencia de una marcación (solo con sesión iniciada). */
    @GetMapping("/reporte/foto/{id}")
    public ResponseEntity<byte[]> verFoto(@PathVariable("id") Long id) {
        java.util.Optional<FotoMarcacion> foto = asistenciaService.buscarFoto(id);
        if (foto.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(foto.get().getTipoContenido()))
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
                .header("X-Content-Type-Options", "nosniff")
                .body(foto.get().getDatos());
    }
}
