@authentication
Feature: User Authentication
  As a BancoDigital user
  I want to log in with my credentials
  So that I can access my account

  @smoke
  Scenario Outline: Login attempt with different credentials
    Given a user attempts to login with document "<document>" and password "<password>"
    Then the login response code should be <statusCode>
    And the login success flag should be <success>

    Examples:
      | document    | password      | statusCode | success |
      | 12345678    | password123   | 200        | true    |
      | 12345678    | wrongpassword | 200        | false   |
      | 99999999    | anypassword   | 200        | false   |

  @smoke
  Scenario: Successful login returns user data
    Given a registered user with document "12345678" and password "password123"
    When the user logs in
    Then the response status code is 200
    And the response contains field "success" with value "true"
    And the response contains field "nombre"
    And the response contains field "numeroCuenta"
    And the response contains field "rol"

  @regression
  Scenario: Login with wrong password returns failure message
    Given a registered user with document "12345678" and password "wrongpassword"
    When the user logs in
    Then the response status code is 200
    And the response contains field "success" with value "false"
    And the response contains field "mensaje" with value "Contraseña incorrecta"
