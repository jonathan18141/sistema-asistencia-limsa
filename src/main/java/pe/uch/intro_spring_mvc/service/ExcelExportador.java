package pe.uch.intro_spring_mvc.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Genera un archivo .xlsx (Excel) sin librerias externas.
 * Un .xlsx es un ZIP con varios XML; aqui se arma una sola hoja con encabezado azul,
 * primera fila fija, filtros automaticos y columnas ajustadas.
 */
public final class ExcelExportador {

    private static final String NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";

    private ExcelExportador() {}

    /**
     * @param nombreHoja  nombre de la pestaña
     * @param encabezados titulos de las columnas
     * @param filas       cada fila es una lista de valores (String, Number o null)
     */
    public static byte[] generar(String nombreHoja, List<String> encabezados, List<List<Object>> filas) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(salida, StandardCharsets.UTF_8)) {
            agregar(zip, "[Content_Types].xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
              + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
              + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
              + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
              + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
              + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
              + "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>"
              + "</Types>");

            agregar(zip, "_rels/.rels",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
              + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
              + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
              + "</Relationships>");

            agregar(zip, "xl/workbook.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
              + "<workbook xmlns=\"" + NS + "\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
              + "<sheets><sheet name=\"" + escapar(nombreHojaValido(nombreHoja)) + "\" sheetId=\"1\" r:id=\"rId1\"/></sheets>"
              + "</workbook>");

            agregar(zip, "xl/_rels/workbook.xml.rels",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
              + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
              + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
              + "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>"
              + "</Relationships>");

            agregar(zip, "xl/styles.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
              + "<styleSheet xmlns=\"" + NS + "\">"
              + "<fonts count=\"2\">"
              +   "<font><sz val=\"11\"/><name val=\"Calibri\"/></font>"
              +   "<font><b/><sz val=\"11\"/><color rgb=\"FFFFFFFF\"/><name val=\"Calibri\"/></font>"
              + "</fonts>"
              + "<fills count=\"3\">"
              +   "<fill><patternFill patternType=\"none\"/></fill>"
              +   "<fill><patternFill patternType=\"gray125\"/></fill>"
              +   "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF2E75B6\"/><bgColor indexed=\"64\"/></patternFill></fill>"
              + "</fills>"
              + "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>"
              + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
              + "<cellXfs count=\"3\">"
              +   "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"
              +   "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyAlignment=\"1\">"
              +     "<alignment horizontal=\"center\" vertical=\"center\"/></xf>"
              +   "<xf numFmtId=\"2\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/>"
              + "</cellXfs>"
              + "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>"
              + "</styleSheet>");

            agregar(zip, "xl/worksheets/sheet1.xml", hoja(encabezados, filas));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return salida.toByteArray();
    }

    // ---------------------------------------------------------------- hoja

    private static String hoja(List<String> encabezados, List<List<Object>> filas) {
        int columnas = encabezados.size();
        int ultimaFila = filas.size() + 1;
        String ultimaColumna = letra(columnas - 1);

        StringBuilder sb = new StringBuilder(4096);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        sb.append("<worksheet xmlns=\"").append(NS).append("\">");
        sb.append("<dimension ref=\"A1:").append(ultimaColumna).append(ultimaFila).append("\"/>");
        sb.append("<sheetViews><sheetView workbookViewId=\"0\">")
          .append("<pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/>")
          .append("<selection pane=\"bottomLeft\"/></sheetView></sheetViews>");
        sb.append("<sheetFormatPr defaultRowHeight=\"15\"/>");

        // Ancho de cada columna segun el texto mas largo
        sb.append("<cols>");
        for (int c = 0; c < columnas; c++) {
            int max = encabezados.get(c).length();
            for (List<Object> fila : filas) {
                if (c < fila.size() && fila.get(c) != null) {
                    max = Math.max(max, fila.get(c).toString().length());
                }
            }
            int ancho = Math.min(50, Math.max(10, max + 3));
            sb.append("<col min=\"").append(c + 1).append("\" max=\"").append(c + 1)
              .append("\" width=\"").append(ancho).append("\" customWidth=\"1\"/>");
        }
        sb.append("</cols>");

        sb.append("<sheetData>");
        // Encabezado
        sb.append("<row r=\"1\" ht=\"22\" customHeight=\"1\">");
        for (int c = 0; c < columnas; c++) {
            celdaTexto(sb, letra(c) + 1, encabezados.get(c), 1);
        }
        sb.append("</row>");
        // Datos
        int numFila = 2;
        for (List<Object> fila : filas) {
            sb.append("<row r=\"").append(numFila).append("\">");
            for (int c = 0; c < columnas && c < fila.size(); c++) {
                Object valor = fila.get(c);
                String ref = letra(c) + numFila;
                if (valor == null) {
                    continue;
                } else if (valor instanceof Number) {
                    // Decimales con 2 cifras (estilo 2); enteros sin formato (estilo 0)
                    boolean decimal = valor instanceof Double || valor instanceof Float
                            || valor instanceof java.math.BigDecimal;
                    sb.append("<c r=\"").append(ref).append("\" s=\"").append(decimal ? 2 : 0).append("\"><v>")
                      .append(valor).append("</v></c>");
                } else {
                    celdaTexto(sb, ref, valor.toString(), 0);
                }
            }
            sb.append("</row>");
            numFila++;
        }
        sb.append("</sheetData>");
        sb.append("<autoFilter ref=\"A1:").append(ultimaColumna).append(ultimaFila).append("\"/>");
        sb.append("</worksheet>");
        return sb.toString();
    }

    private static void celdaTexto(StringBuilder sb, String ref, String texto, int estilo) {
        sb.append("<c r=\"").append(ref).append("\" t=\"inlineStr\" s=\"").append(estilo)
          .append("\"><is><t xml:space=\"preserve\">").append(escapar(texto)).append("</t></is></c>");
    }

    // ---------------------------------------------------------------- utilidades

    /** 0 -> A, 25 -> Z, 26 -> AA ... */
    private static String letra(int indice) {
        StringBuilder sb = new StringBuilder();
        int n = indice;
        while (n >= 0) {
            sb.insert(0, (char) ('A' + n % 26));
            n = n / 26 - 1;
        }
        return sb.toString();
    }

    private static String nombreHojaValido(String nombre) {
        String limpio = nombre == null ? "Hoja1" : nombre.replaceAll("[\\[\\]:*?/\\\\]", " ").trim();
        if (limpio.isEmpty()) {
            limpio = "Hoja1";
        }
        return limpio.length() > 31 ? limpio.substring(0, 31) : limpio;
    }

    /** Escapa caracteres especiales de XML y descarta los caracteres de control invalidos. */
    private static String escapar(String texto) {
        StringBuilder sb = new StringBuilder(texto.length() + 8);
        for (int i = 0; i < texto.length(); i++) {
            char ch = texto.charAt(i);
            switch (ch) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&apos;");
                default -> {
                    boolean valido = ch == '\t' || ch == '\n' || ch == '\r' || ch >= 0x20;
                    if (valido) {
                        sb.append(ch);
                    }
                }
            }
        }
        return sb.toString();
    }

    private static void agregar(ZipOutputStream zip, String ruta, String contenido) throws IOException {
        zip.putNextEntry(new ZipEntry(ruta));
        zip.write(contenido.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}
