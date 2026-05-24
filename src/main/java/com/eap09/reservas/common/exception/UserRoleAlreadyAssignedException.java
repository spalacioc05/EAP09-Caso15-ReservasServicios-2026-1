package com.eap09.reservas.common.exception;

public class UserRoleAlreadyAssignedException extends RuntimeException {

    public UserRoleAlreadyAssignedException(String message) {
        super(message);
    }
}