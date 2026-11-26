package dev.nipponten.domain.models;

// Linha de listagem de pedidos. client segue a regra de OrderDetails.client.
public record OrderSummary(Order order, Client client) {}
