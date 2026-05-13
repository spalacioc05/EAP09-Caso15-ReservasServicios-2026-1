Feature: Actualización de perfil propio 

  Background:
    * url baseUrl
    * def profilePath = '/api/v1/users/me/profile'
    * def randomString = function(s){ return java.util.UUID.randomUUID().toString().substring(0,8) }
    * def setup = callonce read('classpath:com/eap09/reservas/acceptanceTest/provider-auth-setup.feature')
    * def setup1 = callonce read('classpath:com/eap09/reservas/acceptanceTest/auth-setup.feature')
    * def clientEmail = setup1.clientEmail
    * def Authorization = 'Bearer ' + setup.providerToken

  Scenario: Actualización del perfil exitosa
    Given path profilePath
    * def email = 'cliente.' + randomString() + '@udea.edu.co'
    And header Authorization = Authorization
    And request {nombres: 'Camila', apellidos: 'Giraldo', correo: '#(email)' }
    When method patch
    Then status 200
    And match response.message == 'Perfil actualizado correctamente'
    And match response.data.nombres == 'Camila'
    And match response.data.apellidos == 'Giraldo'
    And match response.data.correo == '#(email)'

  Scenario: Correo registrado al enviar el formulario de actualización - excepcional
    Given path profilePath
    And header Authorization = Authorization
    And request { nombres: 'Salome', apellidos: 'Soto', correo: '#(clientEmail)' }
    When method patch
    Then status 401
