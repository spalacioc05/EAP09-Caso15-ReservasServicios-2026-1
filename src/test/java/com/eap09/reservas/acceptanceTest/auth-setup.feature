@ignore
Feature: Configuración de registro y autenticación de un cliente 

  Scenario: Registrar cliente y obtener token
    * url baseUrl
    * def randomString = function(s){ return java.util.UUID.randomUUID().toString().substring(0,8) }
    * def clientEmail = 'test.' + randomString() + '@udea.edu.co'
    * def clientPassword = 'Password123!'

    Given path '/api/v1/clients'
    And request { nombres: 'Test', apellidos: 'User', correo: '#(clientEmail)', contrasena: '#(clientPassword)' }
    When method post
    Then status 201

    Given path '/api/v1/auth/sessions'
    And request { correo: '#(clientEmail)', contrasena: '#(clientPassword)' }
    When method post
    Then status 200

    * def clientToken = response.data.accessToken
    * def Email = clientEmail
