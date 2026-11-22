package dev.nipponten.application.services;

import dev.nipponten.domain.gateways.PaymentGateway;
import dev.nipponten.domain.models.Payment;
import java.time.Instant;
import java.util.Objects;

/** Cópia do pagamento com o que o provedor respondeu. Usado por quem grava respostas do gateway. */
final class PaymentUpdates {

    private PaymentUpdates() {}

    static boolean changes(Payment payment, PaymentGateway.Charge charge) {
        return !Objects.equals(charge.externalId(), payment.externalId())
                || charge.status() != payment.status()
                || !Objects.equals(charge.statusDetail(), payment.statusDetail());
    }

    // O QR code só vem na criação do Pix; nas consultas seguintes o anterior é mantido.
    static Payment withCharge(Payment payment, PaymentGateway.Charge charge, Instant now) {
        boolean approvedNow =
                charge.status() == Payment.Status.APPROVED
                        && payment.status() != Payment.Status.APPROVED;
        return new Payment(
                payment.id(),
                payment.orderId(),
                payment.provider(),
                charge.externalId() == null ? payment.externalId() : charge.externalId(),
                payment.idempotencyKey(),
                payment.method(),
                charge.status(),
                charge.statusDetail(),
                payment.amount(),
                payment.payerEmail(),
                charge.qrCode() == null ? payment.pixQrCode() : charge.qrCode(),
                charge.qrCodeBase64() == null ? payment.pixQrCodeBase64() : charge.qrCodeBase64(),
                payment.expiresAt(),
                approvedNow ? now : payment.approvedAt(),
                payment.createdAt(),
                now);
    }
}
