INSERT INTO public.tbl_tipo_evento (nombre_tipo_evento, descripcion_tipo_evento)
VALUES ('CONSULTA_ADMIN_RESERVAS', 'Consulta administrativa global de reservas de la plataforma')
ON CONFLICT (nombre_tipo_evento) DO NOTHING;