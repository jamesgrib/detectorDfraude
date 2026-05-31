@cards
Feature: Card Management
  As a BancoDigital user
  I want to request and manage my cards
  So that I can make payments and transactions

  # User: 7654321 / ACC-624489
  # Admin: 12345678

  @smoke
  Scenario Outline: Admin decision on card request
    Given the user "7654321" requests a "<cardType>" card
    When the admin "<action>" the card with limit <limit>
    Then the card status should be "<expectedStatus>"

    Examples:
      | cardType | action  | limit   | expectedStatus |
      | CREDITO  | approves| 2000000 | ACTIVA         |
      | DEBITO   | approves| 0       | ACTIVA         |
      | CREDITO  | rejects | 0       | RECHAZADA      |

  @smoke
  Scenario: User requests a debit card — status is PENDIENTE
    Given a user with document "7654321" is authenticated
    When the user requests a "DEBITO" card with holder name "Juan Pablo"
    Then the response status code is 200
    And the card status should be "PENDIENTE"

  @regression
  Scenario: Admin approves a credit card with limit — status is ACTIVA
    Given the user "7654321" requests a "CREDITO" card
    When the admin approves the card with a credit limit of 2000000
    Then the response status code is 200
    And the card status should be "ACTIVA"
    And the card credit limit should be 2000000.0

  @admin @regression
  Scenario: Admin rejects a card with a reason — status is RECHAZADA
    Given the user "7654321" requests a "DEBITO" card
    When the admin rejects the card with reason "Documentación incompleta"
    Then the response status code is 200
    And the card status should be "RECHAZADA"

  @regression
  Scenario: User cancels their own active card
    Given the user "7654321" has an active "DEBITO" card approved by admin
    When the user cancels the card
    Then the response status code is 200
    And the response contains field "mensaje" with value "Tarjeta eliminada exitosamente"
