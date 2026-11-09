-- Opcional: la aplicacion crea estas tablas sola (spring.jpa.hibernate.ddl-auto=update).
-- Usar este script solo si prefieres crear la BD manualmente en MySQL Workbench.
CREATE DATABASE IF NOT EXISTS asistencias
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE asistencias;

CREATE TABLE IF NOT EXISTS empleados (
  dni            VARCHAR(8)   NOT NULL,
  nombre         VARCHAR(100) NOT NULL,
  area           VARCHAR(100) NOT NULL,
  turno_asignado VARCHAR(40)  NOT NULL,
  ubicacion      VARCHAR(10)  NOT NULL,          -- OFICINA | PLANTA
  PRIMARY KEY (dni)
);

CREATE TABLE IF NOT EXISTS marcaciones (
  id               BIGINT       NOT NULL AUTO_INCREMENT,
  dni              VARCHAR(8)   NOT NULL,
  fecha_hora       DATETIME(6)  NOT NULL,
  tipo_marca       VARCHAR(10)  NOT NULL,        -- ENTRADA | SALIDA
  ubicacion        VARCHAR(10)  NOT NULL,
  horas_trabajadas DOUBLE       NULL,
  estado           VARCHAR(40)  NOT NULL,
  fecha_aprobacion DATETIME(6)  NULL,            -- cuando RRHH aprueba una marca de planta
  aprobado_por     VARCHAR(30)  NULL,            -- usuario de RRHH que aprobo
  PRIMARY KEY (id),
  KEY idx_marcaciones_dni_fecha (dni, fecha_hora),
  CONSTRAINT fk_marcaciones_empleado FOREIGN KEY (dni) REFERENCES empleados (dni)
);

-- Foto OBLIGATORIA adjunta en cada marca (entrada y salida) (se guarda en la base de datos)
CREATE TABLE IF NOT EXISTS fotos_marcacion (
  marcacion_id  BIGINT      NOT NULL,
  tipo_contenido VARCHAR(30) NOT NULL,           -- image/jpeg | image/png
  datos         LONGBLOB    NOT NULL,
  fecha_subida  DATETIME(6) NOT NULL,
  desafio       VARCHAR(30) NULL,                -- metodo de verificacion (FOTO_ADJUNTA)
  PRIMARY KEY (marcacion_id)
);

-- Los usuarios (rrhh, gerencia) los crea la aplicacion al arrancar, con la clave cifrada (BCrypt).
CREATE TABLE IF NOT EXISTS usuarios (
  usuario    VARCHAR(30)  NOT NULL,
  clave_hash VARCHAR(100) NOT NULL,
  nombre     VARCHAR(100) NOT NULL,
  rol        VARCHAR(15)  NOT NULL,              -- RRHH | GERENCIA
  PRIMARY KEY (usuario)
);

INSERT IGNORE INTO empleados (dni, nombre, area, turno_asignado, ubicacion) VALUES
 ('71234567', 'Juan Pérez',   'Lab. Metrología / Técnico', '08:00 AM - 05:00 PM', 'PLANTA'),
 ('72987654', 'Carlos Gómez', 'Automatización / Soporte',  '08:00 AM - 05:00 PM', 'OFICINA'),
 ('73456789', 'María Torres', 'Control de Calidad (QA)',   '08:30 AM - 05:30 PM', 'PLANTA');

-- Si ya tenias la tabla fotos_marcacion creada con la columna "datos" chica (error "Data too long for column 'datos'"), ejecuta:
-- ALTER TABLE fotos_marcacion MODIFY datos LONGBLOB NOT NULL;
