INSERT INTO public.tbl_tipo_evento (nombre_tipo_evento, descripcion_tipo_evento)
VALUES ('GENERACION_REPORTE_OPERATIVO', 'Generacion administrativa global de reporte operativo de reservas')
ON CONFLICT (nombre_tipo_evento) DO NOTHING;