package com.eap09.reservas.identityaccess.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tbl_rol")
public class RoleEntity {

    @Id
    @Column(name = "id_rol")
    private Long idRol;

    @Column(name = "nombre_rol", nullable = false, unique = true, length = 50)
    private String nombreRol;

    public Long getIdRol() {
        return idRol;
    }

    public String getNombreRol() {
        return nombreRol;
    }
}
