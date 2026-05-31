@invoices
Feature: Invoice Payment
  As a BancoDigital user
  I want to pay my service invoices
  So that I can keep my services active

  # User: 7654321 / ACC-624489 (saldo: $250,000,000)
  # Admin: 12345678

  @smoke
  Scenario Outline: Invoice payment with different card types
    Given the user "7654321" has a pending invoice and a "<cardType>" card with funds
    When the user pays the invoice with the card
    Then the response status code is 200
    And the invoice status should be "PAGADA"

    Examples:
      | cardType |
      | DEBITO   |
      | CREDITO  |

  @smoke
  Scenario: Generate test invoices for a user
    Given a user with document "7654321" is authenticated
    When the user requests test invoice generation
    Then the response status code is 200
    And the response contains field "mensaje" with value "Facturas generadas"
    And the response contains a non-empty list of "facturas"

  @smoke
  Scenario: Successful invoice payment with debit card
    Given the user "7654321" has a pending invoice and a "DEBITO" card with funds
    When the user pays the invoice with the card
    Then the response status code is 200
    And the invoice status should be "PAGADA"
    And the response contains field "metodoPago" with value "TARJETA"

  @regression
  Scenario: Successful invoice payment with credit card
    Given the user "7654321" has a pending invoice and a "CREDITO" card with funds
    When the user pays the invoice with the card
    Then the response status code is 200
    And the invoice status should be "PAGADA"
    And the response contains field "metodoPago" with value "TARJETA"

  @regression
  Scenario: Invoice payment fails when debit card has no balance
    Given the user "7654321" has a pending invoice and a "DEBITO" card with no balance
    When the user attempts to pay the invoice with the card
    Then the response status code is 400
    And the error message contains "insuficiente"
