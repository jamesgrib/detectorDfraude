@transfers
Feature: Money Transfers
  As a BancoDigital user
  I want to transfer money between accounts
  So that I can send funds to other users

  # User: 7654321 / ACC-624489 (saldo: $250,000,000)
  # Admin: 12345678 / ACC-001

  @smoke
  Scenario Outline: Transfer outcome based on amount
    Given the user "7654321" transfers <amount> from "ACC-624489" to "ACC-001"
    Then the transaction status should be "<expectedStatus>"

    Examples:
      | amount   | expectedStatus |
      | 100000   | APROBADA       |
      | 6000000  | PENDIENTE      |

  @smoke
  Scenario: Successful transfer with sufficient balance
    Given the user "7654321" transfers 100000 from "ACC-624489" to "ACC-001"
    Then the response status code is 200
    And the transaction status should be "APROBADA"

  @regression
  Scenario: Suspicious transfer stays in PENDIENTE state
    Given the user "7654321" transfers 6000000 from "ACC-624489" to "ACC-001"
    Then the response status code is 200
    And the transaction status should be "PENDIENTE"

  @admin @regression
  Scenario: Admin approves a pending transfer
    Given a suspicious transfer is created by "7654321" from "ACC-624489" to "ACC-001"
    When the admin "12345678" approves the transfer
    Then the response status code is 200
    And the transaction status should be "APROBADA"

  @admin @regression
  Scenario: Admin rejects a pending transfer
    Given a suspicious transfer is created by "7654321" from "ACC-624489" to "ACC-001"
    When the admin "12345678" rejects the transfer
    Then the response status code is 200
    And the transaction status should be "RECHAZADA"
