package pe.uch.intro_spring_mvc.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

/**
 * Genera un PDF (A4 horizontal) con una tabla, sin librerias externas.
 * Usa la fuente estandar Helvetica, que todo lector de PDF trae incorporada.
 */
public final class PdfExportador {

    private static final float ANCHO = 841.89f;   // A4 horizontal en puntos
    private static final float ALTO = 595.28f;
    private static final float MARGEN = 36f;
    private static final float ALTO_FILA = 20f;
    private static final float ALTO_ENCABEZADO = 24f;
    private static final float PIE = 40f;          // espacio reservado abajo para el numero de pagina

    // Anchos de Helvetica (milesimas de em) para los caracteres ASCII 32..126
    private static final int[] ANCHOS = {
        278, 278, 355, 556, 556, 889, 667, 191, 333, 333, 389, 584, 278, 333, 278, 278,   // espacio ! " # $ % & ' ( ) * + , - . /
        556, 556, 556, 556, 556, 556, 556, 556, 556, 556,                                   // 0-9
        278, 278, 584, 584, 584, 556, 1015,                                                 // : ; < = > ? @
        667, 667, 722, 722, 667, 611, 778, 722, 278, 500, 667, 556, 833,                    // A-M
        722, 778, 667, 778, 722, 667, 611, 722, 667, 944, 667, 667, 611,                    // N-Z
        278, 278, 278, 469, 556, 333,                                                       // [ \ ] ^ _ `
        556, 556, 500, 556, 556, 278, 556, 556, 222, 222, 500, 222, 833,                    // a-m
        556, 556, 556, 556, 333, 500, 278, 556, 500, 722, 500, 500, 500,                    // n-z
        334, 260, 334, 584                                                                  // { | } ~
    };

    private PdfExportador() {}

    /**
     * @param titulo         titulo grande de la primera pagina
     * @param subtitulo      linea bajo el titulo
     * @param lineasResumen  lineas cortas de texto (filtros, totales...)
     * @param encabezados    titulos de las columnas
     * @param anchos         ancho de cada columna en puntos (la suma debe caber en ~770)
     * @param filas          cada fila es una lista de textos
     */
    public static byte[] generar(String titulo, String subtitulo, List<String> lineasResumen,
                                 List<String> encabezados, float[] anchos, List<List<String>> filas) {
        List<StringBuilder> paginas = new ArrayList<>();

        StringBuilder sb = nuevaPagina(paginas, true, titulo, subtitulo, lineasResumen);
        float y = inicioTabla(lineasResumen);        // distancia desde el borde superior
        y = dibujarEncabezado(sb, encabezados, anchos, y);

        if (filas.isEmpty()) {
            texto(sb, "F1", 11, MARGEN, ALTO - y - 28, "No hay registros con los filtros elegidos.", 0.36f, 0.42f, 0.57f);
        }

        int numFila = 0;
        for (List<String> fila : filas) {
            if (y + ALTO_FILA > ALTO - MARGEN - PIE) {
                sb = nuevaPagina(paginas, false, titulo, subtitulo, lineasResumen);
                y = 62f;
                y = dibujarEncabezado(sb, encabezados, anchos, y);
            }
            dibujarFila(sb, fila, anchos, y, numFila % 2 == 1);
            y += ALTO_FILA;
            numFila++;
        }

        // Pie de pagina: ahora que se sabe el total de paginas
        for (int i = 0; i < paginas.size(); i++) {
            StringBuilder p = paginas.get(i);
            texto(p, "F1", 8, MARGEN, 24, "B&B LIMSA S.A.C.  ·  Control de asistencia", 0.36f, 0.42f, 0.57f);
            String num = "Página " + (i + 1) + " de " + paginas.size();
            texto(p, "F1", 8, ANCHO - MARGEN - ancho(num, 8, false), 24, num, 0.36f, 0.42f, 0.57f);
        }
        return ensamblar(paginas);
    }

    // ---------------------------------------------------------------- paginas

    private static StringBuilder nuevaPagina(List<StringBuilder> paginas, boolean primera, String titulo,
                                             String subtitulo, List<String> resumen) {
        StringBuilder sb = new StringBuilder(8192);
        paginas.add(sb);
        if (primera) {
            relleno(sb, 0, ALTO - 74, ANCHO, 74, 0.114f, 0.306f, 0.847f);                  // banda azul
            texto(sb, "F2", 20, MARGEN, ALTO - 36, titulo, 1f, 1f, 1f);
            texto(sb, "F1", 10, MARGEN, ALTO - 56, subtitulo, 0.86f, 0.92f, 1f);
            float y = 74 + 20;                                                              // desde arriba
            for (String linea : resumen) {
                texto(sb, "F1", 9.5f, MARGEN, ALTO - y, linea, 0.06f, 0.12f, 0.29f);
                y += 14;
            }
        } else {
            relleno(sb, 0, ALTO - 38, ANCHO, 38, 0.114f, 0.306f, 0.847f);
            texto(sb, "F2", 12, MARGEN, ALTO - 24, titulo + " (continuación)", 1f, 1f, 1f);
        }
        return sb;
    }

    private static float inicioTabla(List<String> resumen) {
        return 74 + 20 + resumen.size() * 14 + 12;
    }

    // ---------------------------------------------------------------- tabla

    private static float dibujarEncabezado(StringBuilder sb, List<String> encabezados, float[] anchos, float y) {
        float x = MARGEN;
        float suma = 0;
        for (float a : anchos) {
            suma += a;
        }
        relleno(sb, MARGEN, ALTO - y - ALTO_ENCABEZADO, suma, ALTO_ENCABEZADO, 0.84f, 0.89f, 0.98f);
        for (int c = 0; c < encabezados.size(); c++) {
            String t = ajustar(encabezados.get(c).toUpperCase(), anchos[c] - 12, 7.5f, true);
            texto(sb, "F2", 7.5f, x + 6, ALTO - y - 15.5f, t, 0.12f, 0.23f, 0.54f);
            x += anchos[c];
        }
        return y + ALTO_ENCABEZADO;
    }

    private static void dibujarFila(StringBuilder sb, List<String> fila, float[] anchos, float y, boolean cebra) {
        float suma = 0;
        for (float a : anchos) {
            suma += a;
        }
        if (cebra) {
            relleno(sb, MARGEN, ALTO - y - ALTO_FILA, suma, ALTO_FILA, 0.95f, 0.97f, 1f);
        }
        float x = MARGEN;
        for (int c = 0; c < anchos.length; c++) {
            String valor = c < fila.size() && fila.get(c) != null ? fila.get(c) : "";
            texto(sb, "F1", 8.5f, x + 6, ALTO - y - 13.5f, ajustar(valor, anchos[c] - 12, 8.5f, false), 0.06f, 0.12f, 0.29f);
            x += anchos[c];
        }
        linea(sb, MARGEN, ALTO - y - ALTO_FILA, MARGEN + suma, ALTO - y - ALTO_FILA, 0.83f, 0.88f, 0.97f);
    }

    // ---------------------------------------------------------------- medidas de texto

    private static float anchoCaracter(char c) {
        char base = Normalizer.normalize(String.valueOf(c), Normalizer.Form.NFD).charAt(0);
        if (base >= 32 && base <= 126) {
            return ANCHOS[base - 32];
        }
        return 556;
    }

    private static float ancho(String texto, float tamano, boolean negrita) {
        float total = 0;
        for (int i = 0; i < texto.length(); i++) {
            total += anchoCaracter(texto.charAt(i));
        }
        return total * tamano / 1000f * (negrita ? 1.06f : 1f);
    }

    /** Recorta el texto con "..." si no cabe en el ancho disponible. */
    private static String ajustar(String texto, float disponible, float tamano, boolean negrita) {
        if (ancho(texto, tamano, negrita) <= disponible) {
            return texto;
        }
        String t = texto;
        while (t.length() > 1 && ancho(t + "...", tamano, negrita) > disponible) {
            t = t.substring(0, t.length() - 1);
        }
        return t + "...";
    }

    // ---------------------------------------------------------------- operadores de dibujo

    private static void relleno(StringBuilder sb, float x, float y, float w, float h, float r, float g, float b) {
        sb.append(f(r)).append(' ').append(f(g)).append(' ').append(f(b)).append(" rg ")
          .append(f(x)).append(' ').append(f(y)).append(' ').append(f(w)).append(' ').append(f(h)).append(" re f\n");
    }

    private static void linea(StringBuilder sb, float x1, float y1, float x2, float y2, float r, float g, float b) {
        sb.append(f(r)).append(' ').append(f(g)).append(' ').append(f(b)).append(" RG 0.5 w ")
          .append(f(x1)).append(' ').append(f(y1)).append(" m ")
          .append(f(x2)).append(' ').append(f(y2)).append(" l S\n");
    }

    private static void texto(StringBuilder sb, String fuente, float tamano, float x, float y, String texto,
                              float r, float g, float b) {
        sb.append("BT /").append(fuente).append(' ').append(f(tamano)).append(" Tf ")
          .append(f(r)).append(' ').append(f(g)).append(' ').append(f(b)).append(" rg ")
          .append(f(x)).append(' ').append(f(y)).append(" Td (").append(literal(texto)).append(") Tj ET\n");
    }

    private static String f(float n) {
        return String.format(java.util.Locale.ROOT, "%.2f", n);
    }

    /** Convierte el texto a una cadena PDF en codificacion WinAnsi (tildes y ñ incluidas). */
    private static String literal(String texto) {
        StringBuilder sb = new StringBuilder(texto.length() + 8);
        for (int i = 0; i < texto.length(); i++) {
            int b = winAnsi(texto.charAt(i));
            if (b == '(' || b == ')' || b == '\\') {
                sb.append('\\').append((char) b);
            } else if (b >= 32 && b <= 126) {
                sb.append((char) b);
            } else {
                sb.append('\\').append(String.format("%03o", b));
            }
        }
        return sb.toString();
    }

    private static int winAnsi(char c) {
        if (c >= 32 && c <= 126) {
            return c;
        }
        if (c >= 160 && c <= 255) {
            return c;
        }
        return switch (c) {
            case '\u2014' -> 0x97;   // raya larga
            case '\u2013' -> 0x96;
            case '\u2026' -> 0x85;
            case '\u2022' -> 0x95;
            case '\u2018' -> 0x91;
            case '\u2019' -> 0x92;
            case '\u201C' -> 0x93;
            case '\u201D' -> 0x94;
            case '\u20AC' -> 0x80;
            default -> c < 32 ? ' ' : '?';
        };
    }

    // ---------------------------------------------------------------- estructura del archivo

    private static byte[] ensamblar(List<StringBuilder> paginas) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int total = 4 + paginas.size() * 2 + 1;      // catalogo, paginas, 2 fuentes, (pagina+contenido)*n, info
        long[] posicion = new long[total + 1];
        try {
            escribir(out, "%PDF-1.4\n%\u00e2\u00e3\u00cf\u00d3\n");

            posicion[1] = out.size();
            escribir(out, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

            StringBuilder kids = new StringBuilder();
            for (int i = 0; i < paginas.size(); i++) {
                kids.append(5 + i * 2).append(" 0 R ");
            }
            posicion[2] = out.size();
            escribir(out, "2 0 obj\n<< /Type /Pages /Kids [ " + kids + "] /Count " + paginas.size() + " >>\nendobj\n");

            posicion[3] = out.size();
            escribir(out, "3 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>\nendobj\n");
            posicion[4] = out.size();
            escribir(out, "4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>\nendobj\n");

            for (int i = 0; i < paginas.size(); i++) {
                int idPagina = 5 + i * 2;
                int idContenido = idPagina + 1;
                posicion[idPagina] = out.size();
                escribir(out, idPagina + " 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + f(ANCHO) + " " + f(ALTO)
                        + "] /Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents " + idContenido + " 0 R >>\nendobj\n");

                byte[] contenido = paginas.get(i).toString().getBytes(StandardCharsets.ISO_8859_1);
                posicion[idContenido] = out.size();
                escribir(out, idContenido + " 0 obj\n<< /Length " + contenido.length + " >>\nstream\n");
                out.write(contenido);
                escribir(out, "\nendstream\nendobj\n");
            }

            int idInfo = total;
            posicion[idInfo] = out.size();
            escribir(out, idInfo + " 0 obj\n<< /Title (Reporte de asistencia) /Producer (B&B LIMSA - Control de asistencia) >>\nendobj\n");

            long inicioXref = out.size();
            StringBuilder xref = new StringBuilder("xref\n0 " + (total + 1) + "\n0000000000 65535 f \n");
            for (int id = 1; id <= total; id++) {
                xref.append(String.format("%010d 00000 n \n", posicion[id]));
            }
            escribir(out, xref.toString());
            escribir(out, "trailer\n<< /Size " + (total + 1) + " /Root 1 0 R /Info " + idInfo + " 0 R >>\nstartxref\n"
                    + inicioXref + "\n%%EOF\n");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    private static void escribir(ByteArrayOutputStream out, String s) throws IOException {
        out.write(s.getBytes(StandardCharsets.ISO_8859_1));
    }
}
