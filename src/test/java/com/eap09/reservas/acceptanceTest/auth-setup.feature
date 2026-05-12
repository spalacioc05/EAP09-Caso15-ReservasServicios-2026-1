@ignore
Feature: Configuración de autenticación para pruebas

  Scenario: Registrar cliente y obtener token
    * url baseUrl
    * def randomString = function(s){ return java.util.UUID.randomUUID().toString().substring(0,8) }
    * def testEmail = 'test.' + randomString() + '@udea.edu.co'
    * def testPassword = 'Password123!'

    Given path '/api/v1/clients'
    And request { nombres: 'Test', apellidos: 'User', correo: '#(testEmail)', contrasena: '#(testPassword)' }
    When method post
    Then status 201

    * def authToken = response.data.accessToken
    * def clienteEmail = testEmail
