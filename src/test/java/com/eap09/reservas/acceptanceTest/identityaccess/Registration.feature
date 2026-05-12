Feature: Registro de usuarios

  Background:
    * url baseUrl
    * def clientsPath = '/api/v1/clients'
    * def providersPath = '/api/v1/providers'
    * def randomString = function(s){ return java.util.UUID.randomUUID().toString().substring(0,8) }

  Scenario: Registro exitoso de un cliente
    * def emailCliente = 'cliente.' + randomString() + '@udea.edu.co'
    Given path clientsPath
    And request { nombres: 'Ana', apellidos: 'Lopez', correo: '#(emailCliente)', contrasena: 'Password123!' }
    When method post
    Then status 201
    And match response.message == 'Cliente registrado correctamente'
    And match response.data.correo == '#(emailCliente)'
    And match response.data.rol == 'CLIENTE'

  Scenario: Registro exitoso de un proveedor
    * def email = 'proveedor.' + randomString() + '@udea.edu.co'
    Given path providersPath
    And request { nombres: 'Carlos', apellidos: 'Ramirez', correo: '#(email)', contrasena: 'Password123!' }
    When method post
    Then status 201
    And match response.message == 'Proveedor registrado correctamente'
    And match response.data.correo == '#(email)'
    And match response.data.rol == 'PROVEEDOR'

  Scenario: Registro fallido por correo ya existente
    Given path clientsPath
    And request { nombres: 'Camila', apellidos: 'Monsalve', correo: '#(emailCliente)', contrasena: 'Password123!' }
    When method post
    Then status 400

  Scenario: Registro fallido por contraseña que no cumple políticas
    * def email = 'invalido.' + randomString() + '@udea.edu.co'
    Given path clientsPath
    And request { nombres: 'Laura', apellidos: 'Lopez', correo: '#(email)', contrasena: 'simple' }
    When method post
    Then status 400
