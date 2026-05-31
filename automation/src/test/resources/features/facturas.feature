@invoices
Feature: Invoice Payment
  As a BancoDigital user
  I want to pay my service invoices
  So that I can keep my services active

  @smoke
  Scenario Outline: Invoice payment with different payment methods
    Given a user with document "12345678" has a pending invoice
    And the user has a "<paymentMethod>" with sufficient funds
    When the user pays the invoice using "<paymentMethod>"
    Then the response status code is 200
    And the invoice status should be "PAGADA"

    Examples:
      | paymentMethod  |
      | DEBITO         |
      | CREDITO        |

  @smoke
  Scenario: Generate test invoices for a user
    Given a user with document "12345678" is authenticated
    When the user requests test invoice generation
    Then the response status code is 200
    And the response contains field "mensaje" with value "Facturas generadas"
    And the response contains a non-empty list of "facturas"

  @smoke
  Scenario: Successful invoice payment with debit card
    Given a user with document "12345678" has a pending invoice
    And the user has an active debit card with sufficient balance
    When the user pays the invoice with the debit card
    Then the response status code is 200
    And the invoice status should be "PAGADA"
    And the response contains field "metodoPago" with value "TARJETA"

  @regression
  Scenario: Successful invoice payment with credit card
    Given a user with document "12345678" has a pending invoice
    And the user has an active credit card with available credit
    When the user pays the invoice with the credit card
    Then the response status code is 200
    And the invoice status should be "PAGADA"
    And the response contains field "metodoPago" with value "TARJETA"

  @regression
  Scenario: Invoice payment fails when card has insufficient funds
    Given a user with document "12345678" has a pending invoice
    And the user has a debit card with zero balance
    When the user attempts to pay the invoice with the debit card
    Then the response status code is 400
    And the error message contains "insuficiente"
