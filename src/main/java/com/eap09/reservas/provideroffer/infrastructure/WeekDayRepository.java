package com.eap09.reservas.provideroffer.infrastructure;

import com.eap09.reservas.provideroffer.domain.WeekDayEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeekDayRepository extends JpaRepository<WeekDayEntity, Long> {

    Optional<WeekDayEntity> findByNombreDiaSemana(String nombreDiaSemana);
}
