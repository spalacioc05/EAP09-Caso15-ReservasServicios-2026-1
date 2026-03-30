BEGIN;

INSERT INTO tbl_rol (nombre_rol, descripcion_rol)
VALUES
    ('CLIENTE', 'Usuario que consulta oferta y crea reservas'),
    ('PROVEEDOR', 'Usuario que publica servicios, horarios y disponibilidad'),
    ('ADMINISTRADOR', 'Usuario de control y supervisión del sistema')
ON CONFLICT (nombre_rol) DO NOTHING;

INSERT INTO tbl_categoria_estado (nombre_categoria_estado, descripcion_categoria_estado)
VALUES
    ('tbl_usuario', 'Estados aplicables a la tabla tbl_usuario'),
    ('tbl_servicio', 'Estados aplicables a la tabla tbl_servicio'),
    ('tbl_disponibilidad_servicio', 'Estados aplicables a la tabla tbl_disponibilidad_servicio'),
    ('tbl_reserva', 'Estados aplicables a la tabla tbl_reserva'),
    ('tbl_evento', 'Estados aplicables a la tabla tbl_evento')
ON CONFLICT (nombre_categoria_estado) DO NOTHING;

INSERT INTO tbl_estado (id_categoria_estado, nombre_estado, descripcion_estado)
SELECT ce.id_categoria_estado, x.nombre_estado, x.descripcion_estado
FROM (
    VALUES
        ('tbl_usuario', 'ACTIVA', 'Cuenta habilitada para operar'),
        ('tbl_usuario', 'INACTIVA', 'Cuenta inhabilitada para operar'),

        ('tbl_servicio', 'ACTIVO', 'Servicio disponible para operar'),
        ('tbl_servicio', 'INACTIVO', 'Servicio no disponible para operar'),

        ('tbl_disponibilidad_servicio', 'HABILITADA', 'Franja disponible para reserva'),
        ('tbl_disponibilidad_servicio', 'BLOQUEADA', 'Franja no operable ni reservable'),

        ('tbl_reserva', 'CREADA', 'Reserva creada correctamente en Sprint 1'),

        ('tbl_evento', 'EXITO', 'Evento ejecutado correctamente'),
        ('tbl_evento', 'FALLO', 'Evento que terminó en error o rechazo')
) AS x(nombre_categoria_estado, nombre_estado, descripcion_estado)
JOIN tbl_categoria_estado ce
    ON ce.nombre_categoria_estado = x.nombre_categoria_estado
ON CONFLICT (id_categoria_estado, nombre_estado) DO NOTHING;

INSERT INTO tbl_dia_semana (nombre_dia_semana, orden_dia_semana)
VALUES
    ('LUNES', 1),
    ('MARTES', 2),
    ('MIERCOLES', 3),
    ('JUEVES', 4),
    ('VIERNES', 5),
    ('SABADO', 6),
    ('DOMINGO', 7)
ON CONFLICT (nombre_dia_semana) DO NOTHING;

COMMIT;