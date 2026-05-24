INSERT INTO public.tbl_tipo_evento (nombre_tipo_evento, descripcion_tipo_evento)
VALUES ('ACTUALIZACION_ESTADO_USUARIO', 'Actualizacion administrativa del estado de una cuenta de usuario registrada')
ON CONFLICT (nombre_tipo_evento) DO NOTHING;