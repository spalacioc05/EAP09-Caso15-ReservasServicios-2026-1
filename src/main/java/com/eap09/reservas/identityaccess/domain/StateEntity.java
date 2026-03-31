package com.eap09.reservas.identityaccess.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tbl_estado")
public class StateEntity {

    @Id
    @Column(name = "id_estado")
    private Long idEstado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_categoria_estado", nullable = false)
    private StateCategoryEntity categoriaEstado;

    @Column(name = "nombre_estado", nullable = false)
    private String nombreEstado;

    public Long getIdEstado() {
        return idEstado;
    }

    public void setIdEstado(Long idEstado) {
        this.idEstado = idEstado;
    }

    public StateCategoryEntity getCategoriaEstado() {
        return categoriaEstado;
    }

    public void setCategoriaEstado(StateCategoryEntity categoriaEstado) {
        this.categoriaEstado = categoriaEstado;
    }

    public String getNombreEstado() {
        return nombreEstado;
    }

    public void setNombreEstado(String nombreEstado) {
        this.nombreEstado = nombreEstado;
    }
}
