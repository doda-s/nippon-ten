package dev.nipponten.application.services;

import java.math.BigDecimal;

/**
 * Pedido de envio do e-mail de confirmação, disparado pelo OrderService e entregue ao
 * OrderConfirmationMailer só depois do commit. Fica fora do OrderEvent porque leva o token de
 * acompanhamento em claro, que nunca pode ir para o SSE.
 *
 * @param trackingToken nulo para o cliente com conta, que acompanha o pedido logado
 * @param resent verdadeiro quando é o reenvio do link, que troca o token
 */
public record OrderConfirmation(
        String email,
        String name,
        String code,
        BigDecimal total,
        boolean awaitingPayment,
        String trackingToken,
        boolean resent) {}
