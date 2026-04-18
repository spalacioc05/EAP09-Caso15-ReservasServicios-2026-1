-- =========================================================
-- SQL 3 - SEED OPERATIVO SPRINT 2
-- Ejecutar DESPUES de SQL 1 y SQL 2
-- PostgreSQL / Supabase
-- =========================================================

BEGIN;

-- ---------------------------------------------------------
-- Extension para hashes y UUIDs
-- ---------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =========================================================
-- USUARIOS OPERATIVOS
-- Contraseñas de prueba:
--   admin@reservas.test              -> Admin123!
--   proveedor.alpha@reservas.test    -> Proveedor123!
--   proveedor.beta@reservas.test     -> Proveedor456!
--   cliente.ana@reservas.test        -> Cliente123!
--   cliente.bruno@reservas.test      -> Cliente456!
-- =========================================================

INSERT INTO public.tbl_usuario (
    id_rol,
    id_estado_usuario,
    nombres_usuario,
    apellidos_usuario,
    correo_usuario,
    hash_contrasena_usuario,
    intentos_fallidos_consecutivos,
    fecha_fin_restriccion_acceso
)
SELECT
    r.id_rol,
    e.id_estado,
    v.nombres_usuario,
    v.apellidos_usuario,
    v.correo_usuario::citext,
    crypt(v.plain_password, gen_salt('bf', 10)),
    0,
    NULL
FROM (
    VALUES
        ('ADMINISTRADOR', 'ACTIVA', 'Admin', 'Sistema', 'admin@reservas.test', 'Admin123!'),
        ('PROVEEDOR', 'ACTIVA', 'Paula', 'Proveedor', 'proveedor.alpha@reservas.test', 'Proveedor123!'),
        ('PROVEEDOR', 'ACTIVA', 'Pedro', 'Proveedor', 'proveedor.beta@reservas.test', 'Proveedor456!'),
        ('CLIENTE', 'ACTIVA', 'Ana', 'Cliente', 'cliente.ana@reservas.test', 'Cliente123!'),
        ('CLIENTE', 'ACTIVA', 'Bruno', 'Cliente', 'cliente.bruno@reservas.test', 'Cliente456!')
) AS v(nombre_rol, nombre_estado, nombres_usuario, apellidos_usuario, correo_usuario, plain_password)
JOIN public.tbl_rol r
  ON r.nombre_rol = v.nombre_rol
JOIN public.tbl_estado e
  ON e.nombre_estado = v.nombre_estado
JOIN public.tbl_categoria_estado ce
  ON ce.id_categoria_estado = e.id_categoria_estado
 AND ce.nombre_categoria_estado = 'tbl_usuario';

-- =========================================================
-- HORARIOS GENERALES DE PROVEEDORES
-- =========================================================

INSERT INTO public.tbl_horario_general_proveedor (
    id_usuario_proveedor,
    id_dia_semana,
    hora_inicio,
    hora_fin
)
SELECT
    u.id_usuario,
    d.id_dia_semana,
    s.hora_inicio,
    s.hora_fin
FROM (
    VALUES
        ('proveedor.alpha@reservas.test', TIME '08:00', TIME '18:00'),
        ('proveedor.beta@reservas.test',  TIME '09:00', TIME '17:00')
) AS s(correo_usuario, hora_inicio, hora_fin)
JOIN public.tbl_usuario u
  ON u.correo_usuario = s.correo_usuario::citext
CROSS JOIN public.tbl_dia_semana d;

-- =========================================================
-- SERVICIOS
-- - proveedor.alpha: 2 activos + 1 inactivo
-- - proveedor.beta : 1 activo
-- =========================================================

INSERT INTO public.tbl_servicio (
    id_usuario_proveedor,
    id_estado_servicio,
    nombre_servicio,
    descripcion_servicio,
    duracion_minutos,
    capacidad_maxima_concurrente
)
SELECT
    u.id_usuario,
    e.id_estado,
    v.nombre_servicio,
    v.descripcion_servicio,
    v.duracion_minutos,
    v.capacidad_maxima_concurrente
FROM (
    VALUES
        ('proveedor.alpha@reservas.test', 'ACTIVO',   'CONSULTORIA EXPRESS',    'Servicio base activo del proveedor alpha para pruebas operativas.', 60, 2),
        ('proveedor.alpha@reservas.test', 'ACTIVO',   'SOPORTE REMOTO',         'Servicio activo adicional del proveedor alpha para filtros y finalizacion.', 45, 1),
        ('proveedor.alpha@reservas.test', 'INACTIVO', 'MANTENIMIENTO PREMIUM',  'Servicio inactivo del proveedor alpha para pruebas de activacion.', 90, 1),
        ('proveedor.beta@reservas.test',  'ACTIVO',   'ASESORIA LEGAL BASICA',  'Servicio activo del proveedor beta para pruebas de pertenencia.', 50, 1)
) AS v(correo_proveedor, nombre_estado_servicio, nombre_servicio, descripcion_servicio, duracion_minutos, capacidad_maxima_concurrente)
JOIN public.tbl_usuario u
  ON u.correo_usuario = v.correo_proveedor::citext
JOIN public.tbl_estado e
  ON e.nombre_estado = v.nombre_estado_servicio
JOIN public.tbl_categoria_estado ce
  ON ce.id_categoria_estado = e.id_categoria_estado
 AND ce.nombre_categoria_estado = 'tbl_servicio';

-- =========================================================
-- DISPONIBILIDADES
-- Relativas a la fecha de ejecucion para que sigan sirviendo
-- =========================================================

INSERT INTO public.tbl_disponibilidad_servicio (
    id_servicio,
    id_estado_disponibilidad,
    fecha_disponibilidad,
    hora_inicio,
    hora_fin
)
SELECT
    s.id_servicio,
    e.id_estado,
    CURRENT_DATE + v.desfase_dias,
    v.hora_inicio,
    v.hora_fin
FROM (
    VALUES
        -- CONSULTORIA EXPRESS (proveedor alpha)
        ('proveedor.alpha@reservas.test', 'CONSULTORIA EXPRESS',   'HABILITADA',  2, TIME '09:00', TIME '10:00'),  -- futura con reserva creada
        ('proveedor.alpha@reservas.test', 'CONSULTORIA EXPRESS',   'BLOQUEADA',   2, TIME '10:30', TIME '11:30'),  -- futura bloqueada
        ('proveedor.alpha@reservas.test', 'CONSULTORIA EXPRESS',   'HABILITADA', -2, TIME '08:00', TIME '09:00'),  -- pasada con reserva finalizada

        -- SOPORTE REMOTO (proveedor alpha)
        ('proveedor.alpha@reservas.test', 'SOPORTE REMOTO',        'HABILITADA', -1, TIME '16:00', TIME '17:00'),  -- pasada con reserva creada para finalizar
        ('proveedor.alpha@reservas.test', 'SOPORTE REMOTO',        'HABILITADA',  4, TIME '12:00', TIME '13:00'),  -- futura con reserva cancelada

        -- MANTENIMIENTO PREMIUM (inactivo)
        ('proveedor.alpha@reservas.test', 'MANTENIMIENTO PREMIUM', 'HABILITADA',  3, TIME '14:00', TIME '15:30'),  -- futura habilitada para activar servicio

        -- proveedor beta
        ('proveedor.beta@reservas.test',  'ASESORIA LEGAL BASICA', 'HABILITADA',  3, TIME '11:00', TIME '12:00')   -- futura con reserva creada
) AS v(correo_proveedor, nombre_servicio, nombre_estado_disponibilidad, desfase_dias, hora_inicio, hora_fin)
JOIN public.tbl_usuario u
  ON u.correo_usuario = v.correo_proveedor::citext
JOIN public.tbl_servicio s
  ON s.id_usuario_proveedor = u.id_usuario
 AND s.nombre_servicio = v.nombre_servicio
JOIN public.tbl_estado e
  ON e.nombre_estado = v.nombre_estado_disponibilidad
JOIN public.tbl_categoria_estado ce
  ON ce.id_categoria_estado = e.id_categoria_estado
 AND ce.nombre_categoria_estado = 'tbl_disponibilidad_servicio';

-- =========================================================
-- RESERVAS
-- Casos cubiertos:
-- - CREADA futura (cancelable)
-- - FINALIZADA
-- - CREADA pasada (lista para finalizar por proveedor)
-- - CANCELADA
-- - CREADA de otro proveedor
-- =========================================================

INSERT INTO public.tbl_reserva (
    id_disponibilidad_servicio,
    id_usuario_cliente,
    id_estado_reserva,
    fecha_creacion_reserva,
    fecha_actualizacion_reserva,
    fecha_cancelacion_reserva,
    fecha_finalizacion_reserva
)
SELECT
    ds.id_disponibilidad_servicio,
    uc.id_usuario,
    er.id_estado,
    CURRENT_TIMESTAMP + make_interval(hours => v.offset_horas_creacion),
    COALESCE(
        CASE
            WHEN v.offset_horas_cancelacion IS NOT NULL THEN CURRENT_TIMESTAMP + make_interval(hours => v.offset_horas_cancelacion)
            WHEN v.offset_horas_finalizacion IS NOT NULL THEN CURRENT_TIMESTAMP + make_interval(hours => v.offset_horas_finalizacion)
            ELSE CURRENT_TIMESTAMP + make_interval(hours => v.offset_horas_creacion)
        END,
        CURRENT_TIMESTAMP + make_interval(hours => v.offset_horas_creacion)
    ),
    CASE
        WHEN v.offset_horas_cancelacion IS NOT NULL
        THEN CURRENT_TIMESTAMP + make_interval(hours => v.offset_horas_cancelacion)
        ELSE NULL
    END,
    CASE
        WHEN v.offset_horas_finalizacion IS NOT NULL
        THEN CURRENT_TIMESTAMP + make_interval(hours => v.offset_horas_finalizacion)
        ELSE NULL
    END
FROM (
    VALUES
        -- cliente Ana: reserva futura creada (HU-17 cancelacion exitosa)
        ('cliente.ana@reservas.test',   'proveedor.alpha@reservas.test', 'CONSULTORIA EXPRESS',   2, TIME '09:00', TIME '10:00', 'CREADA',     -48, NULL, NULL),

        -- cliente Ana: reserva ya finalizada
        ('cliente.ana@reservas.test',   'proveedor.alpha@reservas.test', 'CONSULTORIA EXPRESS',  -2, TIME '08:00', TIME '09:00', 'FINALIZADA', -96, NULL, -24),

        -- cliente Ana: reserva creada en franja ya terminada (HU-13 finalizacion posible)
        ('cliente.ana@reservas.test',   'proveedor.alpha@reservas.test', 'SOPORTE REMOTO',       -1, TIME '16:00', TIME '17:00', 'CREADA',     -30, NULL, NULL),

        -- cliente Ana: reserva cancelada
        ('cliente.ana@reservas.test',   'proveedor.alpha@reservas.test', 'SOPORTE REMOTO',        4, TIME '12:00', TIME '13:00', 'CANCELADA',  -24, -2,  NULL),

        -- cliente Bruno: reserva creada en servicio de otro proveedor
        ('cliente.bruno@reservas.test', 'proveedor.beta@reservas.test',  'ASESORIA LEGAL BASICA', 3, TIME '11:00', TIME '12:00', 'CREADA',     -12, NULL, NULL)
) AS v(correo_cliente, correo_proveedor, nombre_servicio, desfase_dias, hora_inicio, hora_fin, nombre_estado_reserva, offset_horas_creacion, offset_horas_cancelacion, offset_horas_finalizacion)
JOIN public.tbl_usuario uc
  ON uc.correo_usuario = v.correo_cliente::citext
JOIN public.tbl_usuario up
  ON up.correo_usuario = v.correo_proveedor::citext
JOIN public.tbl_servicio s
  ON s.id_usuario_proveedor = up.id_usuario
 AND s.nombre_servicio = v.nombre_servicio
JOIN public.tbl_disponibilidad_servicio ds
  ON ds.id_servicio = s.id_servicio
 AND ds.fecha_disponibilidad = CURRENT_DATE + v.desfase_dias
 AND ds.hora_inicio = v.hora_inicio
 AND ds.hora_fin = v.hora_fin
JOIN public.tbl_estado er
  ON er.nombre_estado = v.nombre_estado_reserva
JOIN public.tbl_categoria_estado cer
  ON cer.id_categoria_estado = er.id_categoria_estado
 AND cer.nombre_categoria_estado = 'tbl_reserva';

-- =========================================================
-- SESIONES
-- Casos cubiertos:
-- - activa
-- - cerrada
-- - expirada
-- - revocada
-- =========================================================

INSERT INTO public.tbl_sesion_usuario (
    id_usuario,
    id_estado_sesion,
    jti_token,
    fecha_creacion_sesion,
    fecha_actualizacion_sesion,
    fecha_expiracion_sesion,
    fecha_cierre_sesion,
    direccion_ip,
    user_agent
)
SELECT
    u.id_usuario,
    es.id_estado,
    gen_random_uuid(),
    CURRENT_TIMESTAMP + make_interval(hours => v.offset_horas_creacion),
    COALESCE(
        CASE
            WHEN v.offset_horas_cierre IS NOT NULL THEN CURRENT_TIMESTAMP + make_interval(hours => v.offset_horas_cierre)
            ELSE CURRENT_TIMESTAMP + make_interval(hours => v.offset_horas_creacion)
        END,
        CURRENT_TIMESTAMP + make_interval(hours => v.offset_horas_creacion)
    ),
    CURRENT_TIMESTAMP + make_interval(hours => v.offset_horas_expiracion),
    CASE
        WHEN v.offset_horas_cierre IS NOT NULL
        THEN CURRENT_TIMESTAMP + make_interval(hours => v.offset_horas_cierre)
        ELSE NULL
    END,
    v.direccion_ip::inet,
    v.user_agent
FROM (
    VALUES
        ('cliente.ana@reservas.test',        'ACTIVA',    -1,  10,  NULL, '192.168.10.10', 'PostmanRuntime/7.43.0'),
        ('cliente.ana@reservas.test',        'CERRADA',  -15,   5,   -2, '192.168.10.11', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'),
        ('proveedor.alpha@reservas.test',    'ACTIVA',    -2,   8,  NULL, '192.168.20.10', 'PostmanRuntime/7.43.0'),
        ('proveedor.beta@reservas.test',     'EXPIRADA', -20,  -1,   -1, '192.168.20.11', 'Mozilla/5.0 (Macintosh; Intel Mac OS X)'),
        ('admin@reservas.test',              'REVOCADA',  -6,   6,   -1, '192.168.30.10', 'Insomnia/2024.5')
) AS v(correo_usuario, nombre_estado_sesion, offset_horas_creacion, offset_horas_expiracion, offset_horas_cierre, direccion_ip, user_agent)
JOIN public.tbl_usuario u
  ON u.correo_usuario = v.correo_usuario::citext
JOIN public.tbl_estado es
  ON es.nombre_estado = v.nombre_estado_sesion
JOIN public.tbl_categoria_estado ces
  ON ces.id_categoria_estado = es.id_categoria_estado
 AND ces.nombre_categoria_estado = 'tbl_sesion_usuario';

-- =========================================================
-- EVENTOS MINIMOS DE TRAZABILIDAD
-- =========================================================

INSERT INTO public.tbl_evento (
    id_tipo_evento,
    id_tipo_registro,
    id_estado_evento,
    id_usuario_responsable,
    id_registro_afectado,
    trace_id,
    detalle_evento,
    fecha_evento
)
SELECT
    te.id_tipo_evento,
    tr.id_tipo_registro,
    ee.id_estado,
    x.id_usuario_responsable,
    x.id_registro_afectado,
    gen_random_uuid()::text,
    x.detalle_evento,
    x.fecha_evento
FROM (
    -- Registro proveedor alpha
    SELECT
        'REGISTRO_PROVEEDOR'::varchar AS nombre_tipo_evento,
        'tbl_usuario'::varchar AS nombre_tipo_registro,
        u_alpha.id_usuario AS id_usuario_responsable,
        u_alpha.id_usuario AS id_registro_afectado,
        jsonb_build_object('correo', u_alpha.correo_usuario, 'rol', 'PROVEEDOR') AS detalle_evento,
        NOW() - INTERVAL '5 days' AS fecha_evento
    FROM public.tbl_usuario u_alpha
    WHERE u_alpha.correo_usuario = 'proveedor.alpha@reservas.test'::citext

    UNION ALL

    -- Registro cliente Ana
    SELECT
        'REGISTRO_CLIENTE',
        'tbl_usuario',
        u_ana.id_usuario,
        u_ana.id_usuario,
        jsonb_build_object('correo', u_ana.correo_usuario, 'rol', 'CLIENTE'),
        NOW() - INTERVAL '5 days'
    FROM public.tbl_usuario u_ana
    WHERE u_ana.correo_usuario = 'cliente.ana@reservas.test'::citext

    UNION ALL

    -- Registro servicio Consultoria Express
    SELECT
        'REGISTRO_SERVICIO',
        'tbl_servicio',
        u_alpha.id_usuario,
        s.id_servicio,
        jsonb_build_object('servicio', s.nombre_servicio, 'proveedor', u_alpha.correo_usuario),
        NOW() - INTERVAL '4 days'
    FROM public.tbl_usuario u_alpha
    JOIN public.tbl_servicio s
      ON s.id_usuario_proveedor = u_alpha.id_usuario
     AND s.nombre_servicio = 'CONSULTORIA EXPRESS'
    WHERE u_alpha.correo_usuario = 'proveedor.alpha@reservas.test'::citext

    UNION ALL

    -- Creacion reserva futura de Ana
    SELECT
        'CREACION_RESERVA',
        'tbl_reserva',
        u_ana.id_usuario,
        r.id_reserva,
        jsonb_build_object('cliente', u_ana.correo_usuario, 'estado', 'CREADA'),
        NOW() - INTERVAL '2 days'
    FROM public.tbl_usuario u_ana
    JOIN public.tbl_reserva r
      ON r.id_usuario_cliente = u_ana.id_usuario
    JOIN public.tbl_estado er
      ON er.id_estado = r.id_estado_reserva
    JOIN public.tbl_categoria_estado cer
      ON cer.id_categoria_estado = er.id_categoria_estado
     AND cer.nombre_categoria_estado = 'tbl_reserva'
     AND er.nombre_estado = 'CREADA'
    JOIN public.tbl_disponibilidad_servicio ds
      ON ds.id_disponibilidad_servicio = r.id_disponibilidad_servicio
    JOIN public.tbl_servicio s
      ON s.id_servicio = ds.id_servicio
     AND s.nombre_servicio = 'CONSULTORIA EXPRESS'
    WHERE u_ana.correo_usuario = 'cliente.ana@reservas.test'::citext
      AND ds.fecha_disponibilidad = CURRENT_DATE + 2
      AND ds.hora_inicio = TIME '09:00'
      AND ds.hora_fin = TIME '10:00'

    UNION ALL

    -- Cancelacion reserva
    SELECT
        'CANCELACION_RESERVA',
        'tbl_reserva',
        u_ana.id_usuario,
        r.id_reserva,
        jsonb_build_object('cliente', u_ana.correo_usuario, 'estado', 'CANCELADA'),
        NOW() - INTERVAL '2 hours'
    FROM public.tbl_usuario u_ana
    JOIN public.tbl_reserva r
      ON r.id_usuario_cliente = u_ana.id_usuario
    JOIN public.tbl_estado er
      ON er.id_estado = r.id_estado_reserva
    JOIN public.tbl_categoria_estado cer
      ON cer.id_categoria_estado = er.id_categoria_estado
     AND cer.nombre_categoria_estado = 'tbl_reserva'
     AND er.nombre_estado = 'CANCELADA'
    WHERE u_ana.correo_usuario = 'cliente.ana@reservas.test'::citext

    UNION ALL

    -- Finalizacion reserva
    SELECT
        'FINALIZACION_RESERVA',
        'tbl_reserva',
        u_ana.id_usuario,
        r.id_reserva,
        jsonb_build_object('cliente', u_ana.correo_usuario, 'estado', 'FINALIZADA'),
        NOW() - INTERVAL '1 day'
    FROM public.tbl_usuario u_ana
    JOIN public.tbl_reserva r
      ON r.id_usuario_cliente = u_ana.id_usuario
    JOIN public.tbl_estado er
      ON er.id_estado = r.id_estado_reserva
    JOIN public.tbl_categoria_estado cer
      ON cer.id_categoria_estado = er.id_categoria_estado
     AND cer.nombre_categoria_estado = 'tbl_reserva'
     AND er.nombre_estado = 'FINALIZADA'
    WHERE u_ana.correo_usuario = 'cliente.ana@reservas.test'::citext

    UNION ALL

    -- Cierre de sesion de Ana
    SELECT
        'CIERRE_SESION_USUARIO',
        'tbl_sesion_usuario',
        u_ana.id_usuario,
        su.id_sesion_usuario,
        jsonb_build_object('usuario', u_ana.correo_usuario, 'estado_sesion', 'CERRADA'),
        NOW() - INTERVAL '2 hours'
    FROM public.tbl_usuario u_ana
    JOIN public.tbl_sesion_usuario su
      ON su.id_usuario = u_ana.id_usuario
    JOIN public.tbl_estado es
      ON es.id_estado = su.id_estado_sesion
    JOIN public.tbl_categoria_estado ces
      ON ces.id_categoria_estado = es.id_categoria_estado
     AND ces.nombre_categoria_estado = 'tbl_sesion_usuario'
     AND es.nombre_estado = 'CERRADA'
    WHERE u_ana.correo_usuario = 'cliente.ana@reservas.test'::citext
) AS x
JOIN public.tbl_tipo_evento te
  ON te.nombre_tipo_evento = x.nombre_tipo_evento
JOIN public.tbl_tipo_registro tr
  ON tr.nombre_tipo_registro = x.nombre_tipo_registro
JOIN public.tbl_estado ee
  ON ee.nombre_estado = 'EXITO'
JOIN public.tbl_categoria_estado cee
  ON cee.id_categoria_estado = ee.id_categoria_estado
 AND cee.nombre_categoria_estado = 'tbl_evento';

COMMIT;

-- =========================================================
-- CONSULTAS RAPIDAS DE VERIFICACION (opcionales)
-- Descomenta si quieres revisar despues de ejecutar
-- =========================================================
-- SELECT 'tbl_usuario' AS tabla, COUNT(*) FROM public.tbl_usuario
-- UNION ALL
-- SELECT 'tbl_servicio', COUNT(*) FROM public.tbl_servicio
-- UNION ALL
-- SELECT 'tbl_disponibilidad_servicio', COUNT(*) FROM public.tbl_disponibilidad_servicio
-- UNION ALL
-- SELECT 'tbl_reserva', COUNT(*) FROM public.tbl_reserva
-- UNION ALL
-- SELECT 'tbl_sesion_usuario', COUNT(*) FROM public.tbl_sesion_usuario
-- UNION ALL
-- SELECT 'tbl_evento', COUNT(*) FROM public.tbl_evento;