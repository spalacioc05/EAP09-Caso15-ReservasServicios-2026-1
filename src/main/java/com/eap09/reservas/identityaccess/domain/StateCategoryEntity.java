package com.eap09.reservas.identityaccess.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "tbl_categoria_estado")
public class StateCategoryEntity {

    @Id
    @Column(name = "id_categoria_estado")
    private Long idCategoriaEstado;

    @Column(name = "nombre_categoria_estado", nullable = false, unique = true)
    private String nombreCategoriaEstado;
}
