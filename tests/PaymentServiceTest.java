package com.platform.payment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("PaymentService unit tests")
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private StripeGateway stripeGateway;

    @InjectMocks
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("createPayment: valid request returns succeeded payment")
    void createPayment_validRequest_returnsSucceededPayment() {
        var request = new CreatePaymentRequest(
            UUID.randomUUID(), 4999, "USD", "pm_test_valid"
        );
        var chargeId = "ch_test_123";
        when(stripeGateway.charge(request)).thenReturn(chargeId);
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = paymentService.createPayment(request);

        assertEquals("succeeded", result.getStatus());
        assertEquals(chargeId, result.getStripeChargeId());
        assertEquals(4999, result.getAmountCents());
    }

    @Test
    @DisplayName("createPayment: gateway decline throws PaymentDeclinedException")
    void createPayment_gatewayDeclines_throwsPaymentDeclinedException() {
        var request = new CreatePaymentRequest(
            UUID.randomUUID(), 1000, "USD", "pm_test_decline"
        );
        when(stripeGateway.charge(request)).thenThrow(new StripeDeclineException("Card declined"));

        assertThrows(PaymentDeclinedException.class, () -> paymentService.createPayment(request));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("getPayment: existing id returns payment")
    void getPayment_existingId_returnsPayment() {
        var id = UUID.randomUUID();
        var payment = Payment.builder().id(id).status("succeeded").build();
        when(paymentRepository.findById(id)).thenReturn(Optional.of(payment));

        var result = paymentService.getPayment(id);

        assertEquals(id, result.getId());
    }

    @Test
    @DisplayName("getPayment: unknown id throws PaymentNotFoundException")
    void getPayment_unknownId_throwsNotFoundException() {
        var id = UUID.randomUUID();
        when(paymentRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () -> paymentService.getPayment(id));
    }

    @Test
    @DisplayName("createRefund: amount exceeds original throws RefundAmountException")
    void createRefund_amountExceedsOriginal_throwsRefundAmountException() {
        var paymentId = UUID.randomUUID();
        var payment = Payment.builder().id(paymentId).amountCents(1000).status("succeeded").build();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        var request = new CreateRefundRequest(paymentId, 1500);

        assertThrows(RefundAmountException.class, () -> paymentService.createRefund(request));
    }
}
