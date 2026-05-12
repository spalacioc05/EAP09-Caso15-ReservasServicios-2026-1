@ignore
Feature: Configuración de autenticación para proveedor de pruebas

  Scenario: Registrar proveedor y obtener token
    * url baseUrl
    * def randomString = function(s){ return java.util.UUID.randomUUID().toString().substring(0,8) }
    * def providerEmail = 'proveedor.' + randomString() + '@udea.edu.co'
    * def providerPassword = 'Password123!'

    Given path '/api/v1/providers'
    And request { nombres: 'Juan', apellidos: 'Medina', correo: '#(providerEmail)', contrasena: '#(providerPassword)' }
    When method post
    Then status 201

    Given path '/api/v1/auth/sessions'
    And request { correo: '#(providerEmail)', contrasena: '#(providerPassword)' }
    When method post
    Then status 200

    * def providerToken = response.data.accessToken
    * def providerEmail = providerEmail
