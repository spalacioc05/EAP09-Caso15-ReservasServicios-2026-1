package com.eap09.reservas.identityaccess.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "tbl_usuario")
public class UserAccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long idUsuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_rol", nullable = false)
    private RoleEntity rol;

    @Column(name = "id_estado_usuario", nullable = false)
    private Long idEstado;

    @Column(name = "nombres_usuario", nullable = false, length = 100)
    private String nombresUsuario;

    @Column(name = "apellidos_usuario", nullable = false, length = 100)
    private String apellidosUsuario;

    @Column(name = "correo_usuario", nullable = false, unique = true, length = 120)
    private String correoUsuario;

    @Column(name = "hash_contrasena_usuario", nullable = false, length = 255)
    private String hashContrasenaUsuario;

    @Column(name = "intentos_fallidos_consecutivos", nullable = false)
    private Integer intentosFallidosConsecutivos;

    @Column(name = "fecha_fin_restriccion_acceso")
    private LocalDateTime fechaFinRestriccionAcceso;
}
