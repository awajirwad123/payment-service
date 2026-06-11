@payments @e2e
Feature: Payment Processing

  Background:
    Given the payment-service is running
    And a valid authenticated user with id "user-001"

  Scenario: Successful payment for an order
    Given an order with id "order-001" and total 49.99 USD
    When I POST to "/payments" with order_id "order-001", amount_cents 4999, currency "USD", and token "pm_test_valid"
    Then the response status is 201
    And the response contains a payment "id"
    And the payment "status" is "succeeded"
    And a "payment.completed" event is published to Kafka

  Scenario: Declined card returns 402
    Given an order with id "order-002" and total 19.99 USD
    When I POST to "/payments" with order_id "order-002", amount_cents 1999, currency "USD", and token "pm_test_decline"
    Then the response status is 402
    And the response message contains "Card declined"

  Scenario: Full refund of a successful payment
    Given a succeeded payment with id "pay-001" and amount_cents 4999
    When I POST to "/refunds" with payment_id "pay-001" and no amount_cents
    Then the response status is 201
    And the refund "amount_cents" equals 4999
    And the refund "status" is "succeeded"

  Scenario: Partial refund amount exceeds original
    Given a succeeded payment with id "pay-002" and amount_cents 1000
    When I POST to "/refunds" with payment_id "pay-002" and amount_cents 1500
    Then the response status is 400
    And the response message contains "amount exceeds"

  Scenario: Get payment status by id
    Given a succeeded payment with id "pay-003"
    When I GET "/payments/pay-003"
    Then the response status is 200
    And the response contains "status" equal to "succeeded"
