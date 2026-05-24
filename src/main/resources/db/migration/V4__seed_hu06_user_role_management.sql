INSERT INTO public.tbl_tipo_evento (nombre_tipo_evento, descripcion_tipo_evento)
VALUES ('ACTUALIZACION_ROL_USUARIO', 'Actualizacion administrativa del rol de un usuario registrado')
ON CONFLICT (nombre_tipo_evento) DO NOTHING;