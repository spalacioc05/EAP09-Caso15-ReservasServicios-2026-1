-- =========================================================
-- SQL 1 - RESET TOTAL + CREACION DE ESQUEMA SPRINT 2
-- PostgreSQL / Supabase
-- =========================================================

BEGIN;

-- -----------------------------------------
-- Limpieza previa de procedimientos/funciones
-- -----------------------------------------
DROP PROCEDURE IF EXISTS public.sp_expirar_sesiones_vencidas();
DROP FUNCTION IF EXISTS public.fn_set_fecha_actualizacion();

-- -----------------------------------------
-- Drop de tablas existentes
-- -----------------------------------------
DROP TABLE IF EXISTS public.tbl_evento CASCADE;
DROP TABLE IF EXISTS public.tbl_sesion_usuario CASCADE;
DROP TABLE IF EXISTS public.tbl_reserva CASCADE;
DROP TABLE IF EXISTS public.tbl_disponibilidad_servicio CASCADE;
DROP TABLE IF EXISTS public.tbl_horario_general_proveedor CASCADE;
DROP TABLE IF EXISTS public.tbl_servicio CASCADE;
DROP TABLE IF EXISTS public.tbl_usuario CASCADE;
DROP TABLE IF EXISTS public.tbl_tipo_registro CASCADE;
DROP TABLE IF EXISTS public.tbl_tipo_evento CASCADE;
DROP TABLE IF EXISTS public.tbl_dia_semana CASCADE;
DROP TABLE IF EXISTS public.tbl_estado CASCADE;
DROP TABLE IF EXISTS public.tbl_categoria_estado CASCADE;
DROP TABLE IF EXISTS public.tbl_rol CASCADE;

COMMIT;

-- -----------------------------------------
-- Extensiones necesarias
-- -----------------------------------------
CREATE EXTENSION IF NOT EXISTS citext;
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- =========================================
-- TABLAS CATALOGO
-- =========================================

CREATE TABLE public.tbl_rol (
    id_rol BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre_rol VARCHAR(50) NOT NULL UNIQUE,
    descripcion_rol VARCHAR(255)
);

CREATE TABLE public.tbl_categoria_estado (
    id_categoria_estado BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre_categoria_estado VARCHAR(100) NOT NULL UNIQUE,
    descripcion_categoria_estado VARCHAR(255)
);

CREATE TABLE public.tbl_estado (
    id_estado BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_categoria_estado BIGINT NOT NULL,
    nombre_estado VARCHAR(50) NOT NULL,
    descripcion_estado VARCHAR(255),
    CONSTRAINT fk_tbl_estado_categoria_estado
        FOREIGN KEY (id_categoria_estado)
        REFERENCES public.tbl_categoria_estado(id_categoria_estado),
    CONSTRAINT uq_tbl_estado_categoria_nombre
        UNIQUE (id_categoria_estado, nombre_estado)
);

CREATE TABLE public.tbl_dia_semana (
    id_dia_semana BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre_dia_semana VARCHAR(20) NOT NULL UNIQUE,
    orden_dia_semana INTEGER NOT NULL UNIQUE,
    CONSTRAINT ck_tbl_dia_semana_orden_positivo
        CHECK (orden_dia_semana > 0)
);

CREATE TABLE public.tbl_tipo_evento (
    id_tipo_evento BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre_tipo_evento VARCHAR(100) NOT NULL UNIQUE,
    descripcion_tipo_evento VARCHAR(255)
);

CREATE TABLE public.tbl_tipo_registro (
    id_tipo_registro BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre_tipo_registro VARCHAR(100) NOT NULL UNIQUE,
    descripcion_tipo_registro VARCHAR(255)
);

-- =========================================
-- TABLAS OPERATIVAS
-- =========================================

CREATE TABLE public.tbl_usuario (
    id_usuario BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_rol BIGINT NOT NULL,
    id_estado_usuario BIGINT NOT NULL,
    nombres_usuario VARCHAR(120) NOT NULL,
    apellidos_usuario VARCHAR(120) NOT NULL,
    correo_usuario CITEXT NOT NULL UNIQUE,
    hash_contrasena_usuario VARCHAR(255) NOT NULL,
    intentos_fallidos_consecutivos INTEGER NOT NULL DEFAULT 0,
    fecha_fin_restriccion_acceso TIMESTAMPTZ,
    fecha_creacion_usuario TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fecha_actualizacion_usuario TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_tbl_usuario_rol
        FOREIGN KEY (id_rol)
        REFERENCES public.tbl_rol(id_rol),
    CONSTRAINT fk_tbl_usuario_estado
        FOREIGN KEY (id_estado_usuario)
        REFERENCES public.tbl_estado(id_estado),
    CONSTRAINT ck_tbl_usuario_nombres_no_vacios
        CHECK (BTRIM(nombres_usuario) <> ''),
    CONSTRAINT ck_tbl_usuario_apellidos_no_vacios
        CHECK (BTRIM(apellidos_usuario) <> ''),
    CONSTRAINT ck_tbl_usuario_intentos_no_negativos
        CHECK (intentos_fallidos_consecutivos >= 0)
);

CREATE TABLE public.tbl_horario_general_proveedor (
    id_horario_general_proveedor BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_usuario_proveedor BIGINT NOT NULL,
    id_dia_semana BIGINT NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    fecha_creacion_horario_general TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fecha_actualizacion_horario_general TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_tbl_horario_general_proveedor_usuario
        FOREIGN KEY (id_usuario_proveedor)
        REFERENCES public.tbl_usuario(id_usuario),
    CONSTRAINT fk_tbl_horario_general_proveedor_dia
        FOREIGN KEY (id_dia_semana)
        REFERENCES public.tbl_dia_semana(id_dia_semana),
    CONSTRAINT uq_tbl_horario_general_proveedor_usuario_dia
        UNIQUE (id_usuario_proveedor, id_dia_semana),
    CONSTRAINT ck_tbl_horario_general_rango_valido
        CHECK (hora_inicio < hora_fin)
);

CREATE TABLE public.tbl_servicio (
    id_servicio BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_usuario_proveedor BIGINT NOT NULL,
    id_estado_servicio BIGINT NOT NULL,
    nombre_servicio VARCHAR(150) NOT NULL,
    descripcion_servicio TEXT NOT NULL,
    duracion_minutos INTEGER NOT NULL,
    capacidad_maxima_concurrente INTEGER NOT NULL,
    fecha_creacion_servicio TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fecha_actualizacion_servicio TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_tbl_servicio_usuario_proveedor
        FOREIGN KEY (id_usuario_proveedor)
        REFERENCES public.tbl_usuario(id_usuario),
    CONSTRAINT fk_tbl_servicio_estado
        FOREIGN KEY (id_estado_servicio)
        REFERENCES public.tbl_estado(id_estado),
    CONSTRAINT ck_tbl_servicio_nombre_no_vacio
        CHECK (BTRIM(nombre_servicio) <> ''),
    CONSTRAINT ck_tbl_servicio_descripcion_no_vacia
        CHECK (BTRIM(descripcion_servicio) <> ''),
    CONSTRAINT ck_tbl_servicio_duracion_positiva
        CHECK (duracion_minutos > 0),
    CONSTRAINT ck_tbl_servicio_capacidad_positiva
        CHECK (capacidad_maxima_concurrente > 0)
);

CREATE UNIQUE INDEX uq_tbl_servicio_proveedor_nombre_normalizado
    ON public.tbl_servicio (id_usuario_proveedor, LOWER(BTRIM(nombre_servicio)));

CREATE TABLE public.tbl_disponibilidad_servicio (
    id_disponibilidad_servicio BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_servicio BIGINT NOT NULL,
    id_estado_disponibilidad BIGINT NOT NULL,
    fecha_disponibilidad DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    fecha_creacion_disponibilidad TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fecha_actualizacion_disponibilidad TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_tbl_disponibilidad_servicio_servicio
        FOREIGN KEY (id_servicio)
        REFERENCES public.tbl_servicio(id_servicio),
    CONSTRAINT fk_tbl_disponibilidad_servicio_estado
        FOREIGN KEY (id_estado_disponibilidad)
        REFERENCES public.tbl_estado(id_estado),
    CONSTRAINT ck_tbl_disponibilidad_rango_valido
        CHECK (hora_inicio < hora_fin),
    CONSTRAINT ex_tbl_disponibilidad_servicio_solapamiento
        EXCLUDE USING gist (
            id_servicio WITH =,
            tsrange(
                (fecha_disponibilidad + hora_inicio),
                (fecha_disponibilidad + hora_fin),
                '[)'
            ) WITH &&
        )
);

CREATE TABLE public.tbl_reserva (
    id_reserva BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_disponibilidad_servicio BIGINT NOT NULL,
    id_usuario_cliente BIGINT NOT NULL,
    id_estado_reserva BIGINT NOT NULL,
    fecha_creacion_reserva TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fecha_actualizacion_reserva TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fecha_cancelacion_reserva TIMESTAMPTZ,
    fecha_finalizacion_reserva TIMESTAMPTZ,
    CONSTRAINT fk_tbl_reserva_disponibilidad_servicio
        FOREIGN KEY (id_disponibilidad_servicio)
        REFERENCES public.tbl_disponibilidad_servicio(id_disponibilidad_servicio),
    CONSTRAINT fk_tbl_reserva_usuario_cliente
        FOREIGN KEY (id_usuario_cliente)
        REFERENCES public.tbl_usuario(id_usuario),
    CONSTRAINT fk_tbl_reserva_estado
        FOREIGN KEY (id_estado_reserva)
        REFERENCES public.tbl_estado(id_estado),
    CONSTRAINT ck_tbl_reserva_fechas_exclusivas
        CHECK (
            NOT (
                fecha_cancelacion_reserva IS NOT NULL
                AND fecha_finalizacion_reserva IS NOT NULL
            )
        ),
    CONSTRAINT ck_tbl_reserva_fecha_cancelacion_valida
        CHECK (
            fecha_cancelacion_reserva IS NULL
            OR fecha_cancelacion_reserva >= fecha_creacion_reserva
        ),
    CONSTRAINT ck_tbl_reserva_fecha_finalizacion_valida
        CHECK (
            fecha_finalizacion_reserva IS NULL
            OR fecha_finalizacion_reserva >= fecha_creacion_reserva
        )
);

CREATE TABLE public.tbl_sesion_usuario (
    id_sesion_usuario BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_usuario BIGINT NOT NULL,
    id_estado_sesion BIGINT NOT NULL,
    jti_token UUID NOT NULL UNIQUE,
    fecha_creacion_sesion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fecha_actualizacion_sesion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fecha_expiracion_sesion TIMESTAMPTZ NOT NULL,
    fecha_cierre_sesion TIMESTAMPTZ,
    direccion_ip INET,
    user_agent TEXT,
    CONSTRAINT fk_tbl_sesion_usuario_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES public.tbl_usuario(id_usuario),
    CONSTRAINT fk_tbl_sesion_usuario_estado
        FOREIGN KEY (id_estado_sesion)
        REFERENCES public.tbl_estado(id_estado),
    CONSTRAINT ck_tbl_sesion_expiracion_posterior
        CHECK (fecha_expiracion_sesion > fecha_creacion_sesion),
    CONSTRAINT ck_tbl_sesion_cierre_valido
        CHECK (
            fecha_cierre_sesion IS NULL
            OR fecha_cierre_sesion >= fecha_creacion_sesion
        )
);

CREATE TABLE public.tbl_evento (
    id_evento BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_tipo_evento BIGINT NOT NULL,
    id_tipo_registro BIGINT NOT NULL,
    id_estado_evento BIGINT NOT NULL,
    id_usuario_responsable BIGINT,
    id_registro_afectado BIGINT,
    trace_id VARCHAR(100) NOT NULL,
    detalle_evento JSONB NOT NULL DEFAULT '{}'::jsonb,
    fecha_evento TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_tbl_evento_tipo_evento
        FOREIGN KEY (id_tipo_evento)
        REFERENCES public.tbl_tipo_evento(id_tipo_evento),
    CONSTRAINT fk_tbl_evento_tipo_registro
        FOREIGN KEY (id_tipo_registro)
        REFERENCES public.tbl_tipo_registro(id_tipo_registro),
    CONSTRAINT fk_tbl_evento_estado
        FOREIGN KEY (id_estado_evento)
        REFERENCES public.tbl_estado(id_estado),
    CONSTRAINT fk_tbl_evento_usuario_responsable
        FOREIGN KEY (id_usuario_responsable)
        REFERENCES public.tbl_usuario(id_usuario),
    CONSTRAINT ck_tbl_evento_trace_id_no_vacio
        CHECK (BTRIM(trace_id) <> '')
);

-- =========================================
-- INDICES IMPORTANTES
-- =========================================

CREATE INDEX idx_tbl_servicio_proveedor_estado
    ON public.tbl_servicio (id_usuario_proveedor, id_estado_servicio);

CREATE INDEX idx_tbl_disponibilidad_servicio_fecha_estado
    ON public.tbl_disponibilidad_servicio (id_servicio, fecha_disponibilidad, id_estado_disponibilidad, hora_inicio);

CREATE INDEX idx_tbl_reserva_disponibilidad_estado
    ON public.tbl_reserva (id_disponibilidad_servicio, id_estado_reserva);

CREATE INDEX idx_tbl_reserva_cliente_estado_fecha
    ON public.tbl_reserva (id_usuario_cliente, id_estado_reserva, fecha_creacion_reserva DESC);

CREATE INDEX idx_tbl_sesion_usuario_estado_expiracion
    ON public.tbl_sesion_usuario (id_usuario, id_estado_sesion, fecha_expiracion_sesion DESC);

CREATE INDEX idx_tbl_evento_trace_id
    ON public.tbl_evento (trace_id);

CREATE INDEX idx_tbl_evento_usuario_fecha
    ON public.tbl_evento (id_usuario_responsable, fecha_evento DESC);

-- =========================================
-- FUNCION GENERICA PARA UPDATED_AT
-- =========================================

CREATE OR REPLACE FUNCTION public.fn_set_fecha_actualizacion()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    NEW := jsonb_populate_record(NEW, jsonb_build_object(TG_ARGV[0], NOW()));
    RETURN NEW;
END;
$$;

-- -----------------------------------------
-- Triggers de fecha_actualizacion
-- -----------------------------------------

CREATE TRIGGER tr_tbl_usuario_set_fecha_actualizacion
BEFORE UPDATE ON public.tbl_usuario
FOR EACH ROW
EXECUTE FUNCTION public.fn_set_fecha_actualizacion('fecha_actualizacion_usuario');

CREATE TRIGGER tr_tbl_horario_general_set_fecha_actualizacion
BEFORE UPDATE ON public.tbl_horario_general_proveedor
FOR EACH ROW
EXECUTE FUNCTION public.fn_set_fecha_actualizacion('fecha_actualizacion_horario_general');

CREATE TRIGGER tr_tbl_servicio_set_fecha_actualizacion
BEFORE UPDATE ON public.tbl_servicio
FOR EACH ROW
EXECUTE FUNCTION public.fn_set_fecha_actualizacion('fecha_actualizacion_servicio');

CREATE TRIGGER tr_tbl_disponibilidad_set_fecha_actualizacion
BEFORE UPDATE ON public.tbl_disponibilidad_servicio
FOR EACH ROW
EXECUTE FUNCTION public.fn_set_fecha_actualizacion('fecha_actualizacion_disponibilidad');

CREATE TRIGGER tr_tbl_reserva_set_fecha_actualizacion
BEFORE UPDATE ON public.tbl_reserva
FOR EACH ROW
EXECUTE FUNCTION public.fn_set_fecha_actualizacion('fecha_actualizacion_reserva');

CREATE TRIGGER tr_tbl_sesion_set_fecha_actualizacion
BEFORE UPDATE ON public.tbl_sesion_usuario
FOR EACH ROW
EXECUTE FUNCTION public.fn_set_fecha_actualizacion('fecha_actualizacion_sesion');

-- =========================================
-- PROCEDIMIENTO ALMACENADO
-- Cumple criterio y es util para sesiones
-- =========================================

CREATE OR REPLACE PROCEDURE public.sp_expirar_sesiones_vencidas()
LANGUAGE plpgsql
AS $$
DECLARE
    v_id_estado_activa   BIGINT;
    v_id_estado_expirada BIGINT;
BEGIN
    SELECT e.id_estado
      INTO v_id_estado_activa
      FROM public.tbl_estado e
      JOIN public.tbl_categoria_estado ce
        ON ce.id_categoria_estado = e.id_categoria_estado
     WHERE ce.nombre_categoria_estado = 'tbl_sesion_usuario'
       AND e.nombre_estado = 'ACTIVA';

    SELECT e.id_estado
      INTO v_id_estado_expirada
      FROM public.tbl_estado e
      JOIN public.tbl_categoria_estado ce
        ON ce.id_categoria_estado = e.id_categoria_estado
     WHERE ce.nombre_categoria_estado = 'tbl_sesion_usuario'
       AND e.nombre_estado = 'EXPIRADA';

    IF v_id_estado_activa IS NULL OR v_id_estado_expirada IS NULL THEN
        RAISE EXCEPTION 'No existen los estados base requeridos para tbl_sesion_usuario.';
    END IF;

    UPDATE public.tbl_sesion_usuario
       SET id_estado_sesion = v_id_estado_expirada,
           fecha_cierre_sesion = COALESCE(fecha_cierre_sesion, NOW()),
           fecha_actualizacion_sesion = NOW()
     WHERE id_estado_sesion = v_id_estado_activa
       AND fecha_expiracion_sesion <= NOW();
END;
$$;