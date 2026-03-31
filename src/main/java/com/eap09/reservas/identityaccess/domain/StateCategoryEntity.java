package com.eap09.reservas.identityaccess.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tbl_categoria_estado")
public class StateCategoryEntity {

    @Id
    @Column(name = "id_categoria_estado")
    private Long idCategoriaEstado;

    @Column(name = "nombre_categoria_estado", nullable = false, unique = true)
    private String nombreCategoriaEstado;

    public Long getIdCategoriaEstado() {
        return idCategoriaEstado;
    }

    public void setIdCategoriaEstado(Long idCategoriaEstado) {
        this.idCategoriaEstado = idCategoriaEstado;
    }

    public String getNombreCategoriaEstado() {
        return nombreCategoriaEstado;
    }

    public void setNombreCategoriaEstado(String nombreCategoriaEstado) {
        this.nombreCategoriaEstado = nombreCategoriaEstado;
    }
}
