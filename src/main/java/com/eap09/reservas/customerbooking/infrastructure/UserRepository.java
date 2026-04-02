package com.eap09.reservas.customerbooking.infrastructure;


import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserAccountEntity, Long> {
}