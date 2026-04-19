package com.eap09.reservas.identityaccess.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "tbl_sesion_usuario")
public class UserSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sesion_usuario")
    private Long idSesionUsuario;

    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;

    @Column(name = "id_estado_sesion", nullable = false)
    private Long idEstadoSesion;

    @Column(name = "jti_token", nullable = false, unique = true)
    private UUID jtiToken;

    @Column(name = "fecha_creacion_sesion", nullable = false)
    private OffsetDateTime fechaCreacionSesion;

    @Column(name = "fecha_actualizacion_sesion", nullable = false)
    private OffsetDateTime fechaActualizacionSesion;

    @Column(name = "fecha_expiracion_sesion", nullable = false)
    private OffsetDateTime fechaExpiracionSesion;

    @Column(name = "fecha_cierre_sesion")
    private OffsetDateTime fechaCierreSesion;

    @Column(name = "direccion_ip")
    @JdbcTypeCode(SqlTypes.INET)
    private String direccionIp;

    @Column(name = "user_agent")
    private String userAgent;

    public Long getIdSesionUsuario() {
        return idSesionUsuario;
    }

    public void setIdSesionUsuario(Long idSesionUsuario) {
        this.idSesionUsuario = idSesionUsuario;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public Long getIdEstadoSesion() {
        return idEstadoSesion;
    }

    public void setIdEstadoSesion(Long idEstadoSesion) {
        this.idEstadoSesion = idEstadoSesion;
    }

    public UUID getJtiToken() {
        return jtiToken;
    }

    public void setJtiToken(UUID jtiToken) {
        this.jtiToken = jtiToken;
    }

    public OffsetDateTime getFechaCreacionSesion() {
        return fechaCreacionSesion;
    }

    public void setFechaCreacionSesion(OffsetDateTime fechaCreacionSesion) {
        this.fechaCreacionSesion = fechaCreacionSesion;
    }

    public OffsetDateTime getFechaActualizacionSesion() {
        return fechaActualizacionSesion;
    }

    public void setFechaActualizacionSesion(OffsetDateTime fechaActualizacionSesion) {
        this.fechaActualizacionSesion = fechaActualizacionSesion;
    }

    public OffsetDateTime getFechaExpiracionSesion() {
        return fechaExpiracionSesion;
    }

    public void setFechaExpiracionSesion(OffsetDateTime fechaExpiracionSesion) {
        this.fechaExpiracionSesion = fechaExpiracionSesion;
    }

    public OffsetDateTime getFechaCierreSesion() {
        return fechaCierreSesion;
    }

    public void setFechaCierreSesion(OffsetDateTime fechaCierreSesion) {
        this.fechaCierreSesion = fechaCierreSesion;
    }

    public String getDireccionIp() {
        return direccionIp;
    }

    public void setDireccionIp(String direccionIp) {
        this.direccionIp = direccionIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
}