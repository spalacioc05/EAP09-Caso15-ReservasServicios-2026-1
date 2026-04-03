package com.eap09.reservas.customerbooking.infrastructure;

import com.eap09.reservas.provideroffer.domain.ServiceAvailabilityEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServicesAvailabilityRepository extends JpaRepository<ServiceAvailabilityEntity, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			SELECT sa
			FROM ServiceAvailabilityEntity sa
			WHERE sa.idDisponibilidadServicio = :availabilityId
			""")
	Optional<ServiceAvailabilityEntity> findByIdDisponibilidadServicioForUpdate(@Param("availabilityId") Long availabilityId);
}