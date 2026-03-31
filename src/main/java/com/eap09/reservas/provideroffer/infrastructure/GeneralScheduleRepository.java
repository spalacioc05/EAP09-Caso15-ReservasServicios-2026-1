package com.eap09.reservas.provideroffer.infrastructure;

import com.eap09.reservas.provideroffer.domain.GeneralScheduleEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GeneralScheduleRepository extends JpaRepository<GeneralScheduleEntity, Long> {

    List<GeneralScheduleEntity> findByIdUsuarioProveedorAndDiaSemana_IdDiaSemana(Long idUsuarioProveedor, Long idDiaSemana);
}
