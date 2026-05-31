@transfers
Feature: Money Transfers
  As a BancoDigital user
  I want to transfer money between accounts
  So that I can send funds to other users

  @smoke
  Scenario Outline: Transfer outcome based on amount and balance
    Given the origin account has a balance of <balance>
    And the destination account exists
    When the user transfers <amount> from origin to destination
    Then the transaction status should be "<expectedStatus>"

    Examples:
      | balance    | amount   | expectedStatus |
      | 500000     | 100000   | APROBADA       |
      | 10000000   | 6000000  | PENDIENTE      |
      | 50000      | 100000   | RECHAZADA      |

  @smoke
  Scenario: Successful transfer between accounts with sufficient balance
    Given a user with document "12345678" has an account with sufficient balance
    And a destination account exists
    When the user performs a transfer of 100000
    Then the response status code is 200
    And the transaction status should be "APROBADA"

  @regression
  Scenario: Transfer rejected due to insufficient balance
    Given a user with document "12345678" has an account with insufficient balance
    And a destination account exists
    When the user attempts a transfer of 9999999
    Then the response status code is 400
    And the error message contains "Saldo insuficiente"

  @regression
  Scenario: Suspicious transfer stays in PENDIENTE state
    Given a user with document "12345678" has an account with sufficient balance
    And a destination account exists
    When the user performs a transfer of 6000000
    Then the response status code is 200
    And the transaction status should be "PENDIENTE"

  @admin @regression
  Scenario: Admin approves a pending transfer — funds are transferred
    Given a pending transfer exists with id stored as "pendingTransferId"
    When the admin with document "ADMIN001" approves the transfer
    Then the response status code is 200
    And the transaction status should be "APROBADA"

  @admin @regression
  Scenario: Admin rejects a pending transfer — funds are not moved
    Given a pending transfer exists with id stored as "pendingTransferId"
    When the admin with document "ADMIN001" rejects the transfer
    Then the response status code is 200
    And the transaction status should be "RECHAZADA"
