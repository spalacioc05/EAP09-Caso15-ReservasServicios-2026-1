Feature: Autenticación de usuarios

  Background:
    * url baseUrl
    # callonce: registra el usuario UNA sola vez y comparte el resultado entre todos los escenarios
    * def setup = callonce read('classpath:com/eap09/reservas/acceptanceTest/auth-setup.feature')
    * def testEmail = setup.Email
    * def testPassword = 'Password123!'

  Scenario: Autenticación exitosa de un cliente registrado
    Given path '/api/v1/auth/sessions'
    And request { correo: '#(testEmail)', contrasena: '#(testPassword)' }
    When method post
    Then status 200
    And match response.message == 'Autenticacion exitosa'
    And match response.data.accessToken == '#present'
    And match response.data.tokenType == 'Bearer'
    And match response.data.role == 'CLIENTE'

  Scenario: Autenticación fallida por contraseña incorrecta
    Given path '/api/v1/auth/sessions'
    And request { correo: '#(testEmail)', contrasena: 'WrongPass123!' }
    When method post
    Then status 401
    And match response.message == 'Credenciales no validas'

  Scenario: Autenticación fallida por correo inexistente
    Given path '/api/v1/auth/sessions'
    And request { correo: 'emiliano@udea.edu.co', contrasena: '#(testPassword)' }
    When method post
    Then status 401
    And match response.message == 'Credenciales no validas'

   Scenario: Autenticación fallida por correo vacío
    Given path '/api/v1/auth/sessions'
    And request { correo: '', contrasena: '#(testPassword)' }
    When method post
    Then status 400
