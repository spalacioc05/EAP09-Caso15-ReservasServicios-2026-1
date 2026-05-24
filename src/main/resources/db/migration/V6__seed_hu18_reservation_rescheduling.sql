INSERT INTO public.tbl_tipo_evento (nombre_tipo_evento, descripcion_tipo_evento)
VALUES ('REPROGRAMACION_RESERVA', 'Reprogramacion manual de una reserva propia por parte del cliente')
ON CONFLICT (nombre_tipo_evento) DO NOTHING;