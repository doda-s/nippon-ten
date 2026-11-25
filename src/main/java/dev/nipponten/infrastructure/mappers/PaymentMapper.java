package dev.nipponten.infrastructure.mappers;

import dev.nipponten.domain.models.Payment;
import dev.nipponten.infrastructure.entities.PaymentEntity;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PaymentMapper {

    public PaymentEntity toEntity(Payment model) {
        PaymentEntity entity = new PaymentEntity();
        updateEntity(entity, model);
        return entity;
    }

    public void updateEntity(PaymentEntity entity, Payment model) {
        entity.setOrderId(model.orderId());
        entity.setProvider(PaymentEntity.Provider.valueOf(model.provider().name()));
        entity.setExternalId(model.externalId());
        entity.setIdempotencyKey(model.idempotencyKey());
        entity.setMethod(PaymentEntity.Method.valueOf(model.method().name()));
        entity.setStatus(PaymentEntity.Status.valueOf(model.status().name()));
        entity.setStatusDetail(model.statusDetail());
        entity.setAmount(model.amount());
        entity.setPayerEmail(model.payerEmail());
        entity.setPixQrCode(model.pixQrCode());
        entity.setPixQrCodeBase64(model.pixQrCodeBase64());
        entity.setExpiresAt(model.expiresAt());
        entity.setApprovedAt(model.approvedAt());
        entity.setCreatedAt(model.createdAt());
        entity.setUpdatedAt(model.updatedAt());
    }

    public Payment toModel(PaymentEntity entity) {
        return new Payment(
                entity.getId(),
                entity.getOrderId(),
                Payment.Provider.valueOf(entity.getProvider().name()),
                entity.getExternalId(),
                entity.getIdempotencyKey(),
                Payment.Method.valueOf(entity.getMethod().name()),
                Payment.Status.valueOf(entity.getStatus().name()),
                entity.getStatusDetail(),
                entity.getAmount(),
                entity.getPayerEmail(),
                entity.getPixQrCode(),
                entity.getPixQrCodeBase64(),
                entity.getExpiresAt(),
                entity.getApprovedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
