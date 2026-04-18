-- =========================================================
-- SQL 2 - SEED BASE SPRINT 2
-- Catalogos y datos minimos del sistema
-- =========================================================

BEGIN;

-- -----------------------------------------
-- Roles
-- -----------------------------------------
INSERT INTO public.tbl_rol (nombre_rol, descripcion_rol)
VALUES
    ('CLIENTE', 'Usuario que consulta oferta y crea reservas'),
    ('PROVEEDOR', 'Usuario que publica servicios, horarios y disponibilidad'),
    ('ADMINISTRADOR', 'Usuario de control y supervisión del sistema')
ON CONFLICT (nombre_rol) DO NOTHING;

-- -----------------------------------------
-- Categorias de estado
-- Deben seguir la regla de usar el nombre exacto de la tabla
-- -----------------------------------------
INSERT INTO public.tbl_categoria_estado (nombre_categoria_estado, descripcion_categoria_estado)
VALUES
    ('tbl_usuario', 'Estados aplicables a la tabla tbl_usuario'),
    ('tbl_servicio', 'Estados aplicables a la tabla tbl_servicio'),
    ('tbl_disponibilidad_servicio', 'Estados aplicables a la tabla tbl_disponibilidad_servicio'),
    ('tbl_reserva', 'Estados aplicables a la tabla tbl_reserva'),
    ('tbl_sesion_usuario', 'Estados aplicables a la tabla tbl_sesion_usuario'),
    ('tbl_evento', 'Estados aplicables a la tabla tbl_evento')
ON CONFLICT (nombre_categoria_estado) DO NOTHING;

-- -----------------------------------------
-- Estados base del sistema
-- -----------------------------------------
INSERT INTO public.tbl_estado (id_categoria_estado, nombre_estado, descripcion_estado)
SELECT ce.id_categoria_estado, x.nombre_estado, x.descripcion_estado
FROM (
    VALUES
        -- tbl_usuario
        ('tbl_usuario', 'ACTIVA', 'Cuenta habilitada para operar'),
        ('tbl_usuario', 'INACTIVA', 'Cuenta inhabilitada para operar'),

        -- tbl_servicio
        ('tbl_servicio', 'ACTIVO', 'Servicio disponible para operar'),
        ('tbl_servicio', 'INACTIVO', 'Servicio no disponible para operar'),

        -- tbl_disponibilidad_servicio
        ('tbl_disponibilidad_servicio', 'HABILITADA', 'Franja disponible para reserva'),
        ('tbl_disponibilidad_servicio', 'BLOQUEADA', 'Franja no operable ni reservable'),

        -- tbl_reserva
        ('tbl_reserva', 'CREADA', 'Reserva creada correctamente'),
        ('tbl_reserva', 'CANCELADA', 'Reserva cancelada por el cliente antes del inicio de la franja'),
        ('tbl_reserva', 'FINALIZADA', 'Reserva marcada como atendida y concluida'),

        -- tbl_sesion_usuario
        ('tbl_sesion_usuario', 'ACTIVA', 'Sesion vigente y utilizable'),
        ('tbl_sesion_usuario', 'CERRADA', 'Sesion cerrada explicitamente por logout'),
        ('tbl_sesion_usuario', 'REVOCADA', 'Sesion invalidada por seguridad o control operativo'),
        ('tbl_sesion_usuario', 'EXPIRADA', 'Sesion vencida por tiempo de expiracion'),

        -- tbl_evento
        ('tbl_evento', 'EXITO', 'Evento ejecutado correctamente'),
        ('tbl_evento', 'FALLO', 'Evento que termino en error o rechazo')
) AS x(nombre_categoria_estado, nombre_estado, descripcion_estado)
JOIN public.tbl_categoria_estado ce
  ON ce.nombre_categoria_estado = x.nombre_categoria_estado
ON CONFLICT (id_categoria_estado, nombre_estado) DO NOTHING;

-- -----------------------------------------
-- Dias de la semana
-- -----------------------------------------
INSERT INTO public.tbl_dia_semana (nombre_dia_semana, orden_dia_semana)
VALUES
    ('LUNES', 1),
    ('MARTES', 2),
    ('MIERCOLES', 3),
    ('JUEVES', 4),
    ('VIERNES', 5),
    ('SABADO', 6),
    ('DOMINGO', 7)
ON CONFLICT (nombre_dia_semana) DO NOTHING;

-- -----------------------------------------
-- Tipos de evento
-- Incluye Sprint 1 + Sprint 2
-- -----------------------------------------
INSERT INTO public.tbl_tipo_evento (nombre_tipo_evento, descripcion_tipo_evento)
VALUES
    ('REGISTRO_CLIENTE', 'Registro de una cuenta con rol cliente'),
    ('REGISTRO_PROVEEDOR', 'Registro de una cuenta con rol proveedor'),
    ('AUTENTICACION_USUARIO', 'Intento de autenticacion de un usuario'),
    ('APLICACION_RESTRICCION_ACCESO', 'Aplicacion de restriccion temporal de acceso por intentos fallidos'),
    ('CIERRE_SESION_USUARIO', 'Cierre seguro de la sesion actual del usuario'),
    ('ACTUALIZACION_PERFIL_USUARIO', 'Actualizacion de informacion basica del perfil propio'),
    ('DEFINICION_HORARIO_GENERAL', 'Registro o reemplazo del horario general de atencion del proveedor'),
    ('REGISTRO_SERVICIO', 'Registro de un servicio por parte del proveedor'),
    ('ACTIVACION_SERVICIO', 'Activacion de un servicio propio del proveedor'),
    ('INACTIVACION_SERVICIO', 'Inactivacion de un servicio propio del proveedor'),
    ('CREACION_DISPONIBILIDAD', 'Creacion de una franja de disponibilidad de un servicio'),
    ('BLOQUEO_DISPONIBILIDAD', 'Bloqueo de una franja de disponibilidad no operable'),
    ('CREACION_RESERVA', 'Creacion de una reserva sobre una franja disponible'),
    ('CONSULTA_RESERVAS_PROVEEDOR', 'Consulta operativa de reservas por parte del proveedor'),
    ('FINALIZACION_RESERVA', 'Finalizacion de una reserva atendida por parte del proveedor'),
    ('CANCELACION_RESERVA', 'Cancelacion de una reserva propia por parte del cliente'),
    ('CONSULTA_RESERVAS_CLIENTE', 'Consulta de reservas propias por parte del cliente')
ON CONFLICT (nombre_tipo_evento) DO NOTHING;

-- -----------------------------------------
-- Tipos de registro
-- -----------------------------------------
INSERT INTO public.tbl_tipo_registro (nombre_tipo_registro, descripcion_tipo_registro)
VALUES
    ('tbl_usuario', 'Registro de la tabla tbl_usuario'),
    ('tbl_horario_general_proveedor', 'Registro de la tabla tbl_horario_general_proveedor'),
    ('tbl_servicio', 'Registro de la tabla tbl_servicio'),
    ('tbl_disponibilidad_servicio', 'Registro de la tabla tbl_disponibilidad_servicio'),
    ('tbl_reserva', 'Registro de la tabla tbl_reserva'),
    ('tbl_sesion_usuario', 'Registro de la tabla tbl_sesion_usuario')
ON CONFLICT (nombre_tipo_registro) DO NOTHING;

COMMIT;