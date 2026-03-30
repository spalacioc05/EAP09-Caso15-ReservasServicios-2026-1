-- WARNING: This schema is for context only and is not meant to be run.
-- Table order and constraints may not be valid for execution.

CREATE TABLE public.tbl_categoria_estado (
  id_categoria_estado bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  nombre_categoria_estado character varying NOT NULL UNIQUE,
  descripcion_categoria_estado character varying,
  CONSTRAINT tbl_categoria_estado_pkey PRIMARY KEY (id_categoria_estado)
);
CREATE TABLE public.tbl_dia_semana (
  id_dia_semana bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  nombre_dia_semana character varying NOT NULL UNIQUE,
  orden_dia_semana integer NOT NULL UNIQUE CHECK (orden_dia_semana > 0),
  CONSTRAINT tbl_dia_semana_pkey PRIMARY KEY (id_dia_semana)
);
CREATE TABLE public.tbl_disponibilidad_servicio (
  id_disponibilidad_servicio bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  id_servicio bigint NOT NULL,
  id_estado_disponibilidad bigint NOT NULL,
  fecha_disponibilidad date NOT NULL,
  hora_inicio time without time zone NOT NULL,
  hora_fin time without time zone NOT NULL,
  fecha_creacion_disponibilidad timestamp with time zone NOT NULL DEFAULT now(),
  fecha_actualizacion_disponibilidad timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT tbl_disponibilidad_servicio_pkey PRIMARY KEY (id_disponibilidad_servicio),
  CONSTRAINT fk_tbl_disponibilidad_servicio_servicio FOREIGN KEY (id_servicio) REFERENCES public.tbl_servicio(id_servicio),
  CONSTRAINT fk_tbl_disponibilidad_servicio_estado FOREIGN KEY (id_estado_disponibilidad) REFERENCES public.tbl_estado(id_estado)
);
CREATE TABLE public.tbl_estado (
  id_estado bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  id_categoria_estado bigint NOT NULL,
  nombre_estado character varying NOT NULL,
  descripcion_estado character varying,
  CONSTRAINT tbl_estado_pkey PRIMARY KEY (id_estado),
  CONSTRAINT fk_tbl_estado_categoria_estado FOREIGN KEY (id_categoria_estado) REFERENCES public.tbl_categoria_estado(id_categoria_estado)
);
CREATE TABLE public.tbl_evento (
  id_evento bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  id_tipo_evento bigint NOT NULL,
  id_tipo_registro bigint NOT NULL,
  id_estado_evento bigint NOT NULL,
  id_usuario_responsable bigint,
  id_registro_afectado bigint,
  trace_id character varying NOT NULL CHECK (btrim(trace_id::text) <> ''::text),
  detalle_evento jsonb NOT NULL DEFAULT '{}'::jsonb,
  fecha_evento timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT tbl_evento_pkey PRIMARY KEY (id_evento),
  CONSTRAINT fk_tbl_evento_tipo_evento FOREIGN KEY (id_tipo_evento) REFERENCES public.tbl_tipo_evento(id_tipo_evento),
  CONSTRAINT fk_tbl_evento_tipo_registro FOREIGN KEY (id_tipo_registro) REFERENCES public.tbl_tipo_registro(id_tipo_registro),
  CONSTRAINT fk_tbl_evento_estado FOREIGN KEY (id_estado_evento) REFERENCES public.tbl_estado(id_estado),
  CONSTRAINT fk_tbl_evento_usuario_responsable FOREIGN KEY (id_usuario_responsable) REFERENCES public.tbl_usuario(id_usuario)
);
CREATE TABLE public.tbl_horario_general_proveedor (
  id_horario_general_proveedor bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  id_usuario_proveedor bigint NOT NULL,
  id_dia_semana bigint NOT NULL,
  hora_inicio time without time zone NOT NULL,
  hora_fin time without time zone NOT NULL,
  fecha_creacion_horario_general timestamp with time zone NOT NULL DEFAULT now(),
  fecha_actualizacion_horario_general timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT tbl_horario_general_proveedor_pkey PRIMARY KEY (id_horario_general_proveedor),
  CONSTRAINT fk_tbl_horario_general_proveedor_usuario FOREIGN KEY (id_usuario_proveedor) REFERENCES public.tbl_usuario(id_usuario),
  CONSTRAINT fk_tbl_horario_general_proveedor_dia FOREIGN KEY (id_dia_semana) REFERENCES public.tbl_dia_semana(id_dia_semana)
);
CREATE TABLE public.tbl_reserva (
  id_reserva bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  id_disponibilidad_servicio bigint NOT NULL,
  id_usuario_cliente bigint NOT NULL,
  id_estado_reserva bigint NOT NULL,
  fecha_creacion_reserva timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT tbl_reserva_pkey PRIMARY KEY (id_reserva),
  CONSTRAINT fk_tbl_reserva_disponibilidad_servicio FOREIGN KEY (id_disponibilidad_servicio) REFERENCES public.tbl_disponibilidad_servicio(id_disponibilidad_servicio),
  CONSTRAINT fk_tbl_reserva_usuario_cliente FOREIGN KEY (id_usuario_cliente) REFERENCES public.tbl_usuario(id_usuario),
  CONSTRAINT fk_tbl_reserva_estado FOREIGN KEY (id_estado_reserva) REFERENCES public.tbl_estado(id_estado)
);
CREATE TABLE public.tbl_rol (
  id_rol bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  nombre_rol character varying NOT NULL UNIQUE,
  descripcion_rol character varying,
  CONSTRAINT tbl_rol_pkey PRIMARY KEY (id_rol)
);
CREATE TABLE public.tbl_servicio (
  id_servicio bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  id_usuario_proveedor bigint NOT NULL,
  id_estado_servicio bigint NOT NULL,
  nombre_servicio character varying NOT NULL CHECK (btrim(nombre_servicio::text) <> ''::text),
  descripcion_servicio text NOT NULL CHECK (btrim(descripcion_servicio) <> ''::text),
  duracion_minutos integer NOT NULL CHECK (duracion_minutos > 0),
  capacidad_maxima_concurrente integer NOT NULL CHECK (capacidad_maxima_concurrente > 0),
  fecha_creacion_servicio timestamp with time zone NOT NULL DEFAULT now(),
  fecha_actualizacion_servicio timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT tbl_servicio_pkey PRIMARY KEY (id_servicio),
  CONSTRAINT fk_tbl_servicio_usuario_proveedor FOREIGN KEY (id_usuario_proveedor) REFERENCES public.tbl_usuario(id_usuario),
  CONSTRAINT fk_tbl_servicio_estado FOREIGN KEY (id_estado_servicio) REFERENCES public.tbl_estado(id_estado)
);
CREATE TABLE public.tbl_tipo_evento (
  id_tipo_evento bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  nombre_tipo_evento character varying NOT NULL UNIQUE,
  descripcion_tipo_evento character varying,
  CONSTRAINT tbl_tipo_evento_pkey PRIMARY KEY (id_tipo_evento)
);
CREATE TABLE public.tbl_tipo_registro (
  id_tipo_registro bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  nombre_tipo_registro character varying NOT NULL UNIQUE,
  descripcion_tipo_registro character varying,
  CONSTRAINT tbl_tipo_registro_pkey PRIMARY KEY (id_tipo_registro)
);
CREATE TABLE public.tbl_usuario (
  id_usuario bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  id_rol bigint NOT NULL,
  id_estado_usuario bigint NOT NULL,
  nombres_usuario character varying NOT NULL CHECK (btrim(nombres_usuario::text) <> ''::text),
  apellidos_usuario character varying NOT NULL CHECK (btrim(apellidos_usuario::text) <> ''::text),
  correo_usuario USER-DEFINED NOT NULL UNIQUE,
  hash_contrasena_usuario character varying NOT NULL,
  intentos_fallidos_consecutivos integer NOT NULL DEFAULT 0 CHECK (intentos_fallidos_consecutivos >= 0),
  fecha_fin_restriccion_acceso timestamp with time zone,
  fecha_creacion_usuario timestamp with time zone NOT NULL DEFAULT now(),
  fecha_actualizacion_usuario timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT tbl_usuario_pkey PRIMARY KEY (id_usuario),
  CONSTRAINT fk_tbl_usuario_rol FOREIGN KEY (id_rol) REFERENCES public.tbl_rol(id_rol),
  CONSTRAINT fk_tbl_usuario_estado FOREIGN KEY (id_estado_usuario) REFERENCES public.tbl_estado(id_estado)
);