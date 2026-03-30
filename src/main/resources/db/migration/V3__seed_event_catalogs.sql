BEGIN;

INSERT INTO tbl_tipo_evento (nombre_tipo_evento, descripcion_tipo_evento)
VALUES
    ('REGISTRO_CLIENTE', 'Registro de una cuenta con rol cliente'),
    ('REGISTRO_PROVEEDOR', 'Registro de una cuenta con rol proveedor'),
    ('AUTENTICACION_USUARIO', 'Intento de autenticación de un usuario'),
    ('APLICACION_RESTRICCION_ACCESO', 'Aplicación de restricción temporal de acceso por intentos fallidos'),
    ('DEFINICION_HORARIO_GENERAL', 'Registro o reemplazo del horario general de atención del proveedor'),
    ('REGISTRO_SERVICIO', 'Registro de un servicio por parte del proveedor'),
    ('CREACION_DISPONIBILIDAD', 'Creación de una franja de disponibilidad de un servicio'),
    ('BLOQUEO_DISPONIBILIDAD', 'Bloqueo de una franja de disponibilidad no operable'),
    ('CREACION_RESERVA', 'Creación de una reserva sobre una franja disponible')
ON CONFLICT (nombre_tipo_evento) DO NOTHING;

INSERT INTO tbl_tipo_registro (nombre_tipo_registro, descripcion_tipo_registro)
VALUES
    ('tbl_usuario', 'Registro de la tabla tbl_usuario'),
    ('tbl_horario_general_proveedor', 'Registro de la tabla tbl_horario_general_proveedor'),
    ('tbl_servicio', 'Registro de la tabla tbl_servicio'),
    ('tbl_disponibilidad_servicio', 'Registro de la tabla tbl_disponibilidad_servicio'),
    ('tbl_reserva', 'Registro de la tabla tbl_reserva')
ON CONFLICT (nombre_tipo_registro) DO NOTHING;

COMMIT;