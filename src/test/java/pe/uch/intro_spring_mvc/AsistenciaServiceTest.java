package pe.uch.intro_spring_mvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import pe.uch.intro_spring_mvc.model.Empleado;
import pe.uch.intro_spring_mvc.model.RegistroAsistencia;
import pe.uch.intro_spring_mvc.service.AsistenciaException;
import pe.uch.intro_spring_mvc.service.AsistenciaService;

@SpringBootTest
@Transactional
class AsistenciaServiceTest {

    private static final String DNI_PLANTA = "71234567";   // Juan Pérez (PLANTA)
    private static final String DNI_OFICINA = "72987654";  // se crea dentro de la prueba

    private static final String DESAFIO = "FOTO_ADJUNTA";
    // JPEG real de 320x320 generado en memoria (la validacion lo decodifica)
    private static final byte[] FOTO_JPG = generarJpeg(320, 320);

    private static byte[] generarJpeg(int ancho, int alto) {
        try {
            BufferedImage img = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, "jpg", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private RegistroAsistencia marcar(String dni) {
        return service.marcar(dni, FOTO_JPG, DESAFIO);
    }

    private void crearEmpleadoOficina() {
        service.registrarEmpleado(DNI_OFICINA, "Carlos Gómez", "Soporte", "08:00 AM - 05:00 PM", "OFICINA");
    }

    @Autowired
    private AsistenciaService service;

    @Test
    void primeraMarcaDelDiaEsEntrada() {
        RegistroAsistencia r = marcar(DNI_PLANTA);
        assertEquals("ENTRADA", r.getTipoMarca());
        assertEquals(AsistenciaService.PENDIENTE_PLANTA, r.getEstado());
    }

    @Test
    void segundaMarcaDelDiaEsSalida() {
        marcar(DNI_PLANTA);
        RegistroAsistencia r = marcar(DNI_PLANTA);
        assertEquals("SALIDA", r.getTipoMarca());
    }

    @Test
    void terceraMarcaDelDiaSeRechaza() {
        marcar(DNI_PLANTA);
        marcar(DNI_PLANTA);
        assertThrows(AsistenciaException.class, () -> marcar(DNI_PLANTA));
    }

    @Test
    void dniInexistenteSeRechaza() {
        AsistenciaException e = assertThrows(AsistenciaException.class, () -> marcar("00000000"));
        assertEquals("El DNI 00000000 no existe en el sistema.", e.getMessage());
    }

    @Test
    void dniConFormatoInvalidoSeRechaza() {
        assertThrows(AsistenciaException.class, () -> marcar("123"));
        assertThrows(AsistenciaException.class, () -> marcar("ABCDEFGH"));
        assertThrows(AsistenciaException.class, () -> marcar(""));
    }

    @Test
    void salidaDeOficinaSinJornadaCompletaQuedaObservada() {
        crearEmpleadoOficina();
        marcar(DNI_OFICINA);
        RegistroAsistencia r = marcar(DNI_OFICINA);
        assertEquals(AsistenciaService.OBSERVADO, r.getEstado());
    }

    @Test
    void indicadorDePlantaCuentaSoloMarcasDePlanta() {
        crearEmpleadoOficina();
        marcar(DNI_PLANTA);       // planta
        marcar(DNI_OFICINA);      // oficina
        assertTrue(Math.abs(service.porcentajePlanta() - 50.0) < 0.001);
    }

    @Test
    void aprobarSalidaDePlantaIncompletaQuedaObservadaConFecha() {
        marcar(DNI_PLANTA);
        RegistroAsistencia salida = marcar(DNI_PLANTA);
        assertEquals(AsistenciaService.PENDIENTE_PLANTA, salida.getEstado());

        RegistroAsistencia aprobada = service.aprobar(salida.getId(), "rrhh");
        assertEquals(AsistenciaService.OBSERVADO, aprobada.getEstado());
        assertTrue(aprobada.getFechaAprobacion() != null);
        assertEquals("rrhh", aprobada.getAprobadoPor());
    }

    @Test
    void aprobarMarcaQueNoEstaPendienteSeRechaza() {
        crearEmpleadoOficina();
        RegistroAsistencia oficina = marcar(DNI_OFICINA);   // OFICINA -> EN_JORNADA
        assertThrows(AsistenciaException.class, () -> service.aprobar(oficina.getId(), "rrhh"));
    }

    @Test
    void registrarEmpleadoNuevoPermiteMarcar() {
        Empleado e = service.registrarEmpleado("74561239", "Ana Ruiz", "Almacén", "08:00 AM - 05:00 PM", "planta");
        assertEquals("PLANTA", e.getUbicacion());
        assertEquals("ENTRADA", marcar("74561239").getTipoMarca());
    }

    @Test
    void registrarEmpleadoConDatosInvalidosSeRechaza() {
        assertThrows(AsistenciaException.class,
                () -> service.registrarEmpleado("71234567", "Repetido", "Área", "Turno", "PLANTA"));   // DNI duplicado
        assertThrows(AsistenciaException.class,
                () -> service.registrarEmpleado("123", "Ana", "Área", "Turno", "PLANTA"));             // DNI corto
        assertThrows(AsistenciaException.class,
                () -> service.registrarEmpleado("74561239", " ", "Área", "Turno", "PLANTA"));          // nombre vacío
        assertThrows(AsistenciaException.class,
                () -> service.registrarEmpleado("74561239", "Ana", "Área", "Turno", "OTRA"));          // ubicación inválida
    }

    @Test
    void filtrarPorUbicacionDevuelveSoloEsaUbicacion() {
        crearEmpleadoOficina();
        marcar(DNI_PLANTA);
        marcar(DNI_OFICINA);
        List<RegistroAsistencia> oficina = service.filtrar(null, null, "OFICINA", "");
        assertEquals(1, oficina.size());
        assertEquals("OFICINA", oficina.get(0).getUbicacion());
    }

    @Test
    void eliminarEmpleadoSinMarcacionesFunciona() {
        crearEmpleadoOficina();
        Empleado eliminado = service.eliminarEmpleado(DNI_OFICINA);
        assertEquals("Carlos Gómez", eliminado.getNombre());
        assertThrows(AsistenciaException.class, () -> marcar(DNI_OFICINA));
    }

    @Test
    void eliminarEmpleadoConMarcacionesSeRechaza() {
        marcar(DNI_PLANTA);
        assertThrows(AsistenciaException.class, () -> service.eliminarEmpleado(DNI_PLANTA));
    }

    @Test
    void excelSeGeneraComoArchivoXlsxValido() {
        marcar(DNI_PLANTA);
        byte[] excel = service.generarExcel(null, null, "", "");
        // Un .xlsx es un ZIP: empieza con las letras "PK"
        assertTrue(excel.length > 500);
        assertEquals('P', (char) excel[0]);
        assertEquals('K', (char) excel[1]);
    }

    @Test
    void marcarConFotoGuardaLaEvidenciaYElDesafio() {
        RegistroAsistencia r = marcar(DNI_PLANTA);
        assertTrue(service.buscarFoto(r.getId()).isPresent());
        assertEquals("image/jpeg", service.buscarFoto(r.getId()).get().getTipoContenido());
        assertEquals(DESAFIO, service.buscarFoto(r.getId()).get().getDesafio());
        assertTrue(service.idsConFoto().contains(r.getId()));
    }

    @Test
    void entradaYSalidaGuardanCadaUnaSuFoto() {
        RegistroAsistencia entrada = marcar(DNI_PLANTA);
        RegistroAsistencia salida = marcar(DNI_PLANTA);
        assertTrue(service.buscarFoto(entrada.getId()).isPresent());
        assertTrue(service.buscarFoto(salida.getId()).isPresent());
    }

    @Test
    void marcarSinFotoSeRechazaEnEntradaYEnSalida() {
        assertThrows(AsistenciaException.class, () -> service.marcar(DNI_PLANTA, null, DESAFIO));
        assertThrows(AsistenciaException.class, () -> service.marcar(DNI_PLANTA, new byte[0], DESAFIO));
        marcar(DNI_PLANTA);   // entrada con foto
        AsistenciaException e = assertThrows(AsistenciaException.class,
                () -> service.marcar(DNI_PLANTA, null, DESAFIO)); // salida sin foto
        assertTrue(e.getMessage().contains("obligatoria"));
    }

    @Test
    void archivoQueNoEsImagenSeRechaza() {
        AsistenciaException e = assertThrows(AsistenciaException.class,
                () -> service.marcar(DNI_PLANTA, "esto no es una foto".getBytes(), DESAFIO));
        assertEquals("La foto debe ser una imagen JPG o PNG.", e.getMessage());
    }

    @Test
    void imagenDanadaOMuyPequenaSeRechaza() {
        byte[] danada = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F', 0, 1};
        assertThrows(AsistenciaException.class, () -> service.marcar(DNI_PLANTA, danada, DESAFIO));
        byte[] pequena = generarJpeg(50, 50);
        assertThrows(AsistenciaException.class, () -> service.marcar(DNI_PLANTA, pequena, DESAFIO));
    }

    @Test
    void pdfSeGeneraComoArchivoValido() {
        marcar(DNI_PLANTA);
        byte[] pdf = service.generarPdf(null, null, "", "", "Prueba (RRHH)");
        assertTrue(pdf.length > 500);
        assertEquals("%PDF", new String(pdf, 0, 4));
    }
}
