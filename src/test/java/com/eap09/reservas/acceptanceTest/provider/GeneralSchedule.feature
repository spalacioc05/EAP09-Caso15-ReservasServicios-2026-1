Feature: Definición de horario general de atención

  Background:
    * url baseUrl
    * def schedulePath = '/api/v1/providers/me/general-schedule'
    * def setup = callonce read('classpath:com/eap09/reservas/acceptanceTest/provider-auth-setup.feature')
    * def Authorization = 'Bearer ' + setup.providerToken

  Scenario: Definición exitosa del horario general para el día lunes
    Given path schedulePath + '/LUNES'
    And header Authorization = Authorization
    And request { horaInicio: '08:00:00', horaFin: '12:00:00' }
    When method put
    Then status 200
    And match response.message == 'Horario general definido correctamente'
    And match response.data.dayOfWeek == 'LUNES'
    And match response.data.horaInicio == '08:00:00'
    And match response.data.horaFin == '12:00:00'
    And match response.data.providerUserId == '#number'

  Scenario: Actualización de horario existente para el día lunes
    Given path schedulePath + '/LUNES'
    And header Authorization = Authorization
    And request { horaInicio: '09:00:00', horaFin: '13:00:00' }
    When method put
    Then status 200
    And match response.message == 'Horario general definido correctamente'
    And match response.data.dayOfWeek == 'LUNES'
    And match response.data.horaInicio == '09:00:00'
    And match response.data.horaFin == '13:00:00'
