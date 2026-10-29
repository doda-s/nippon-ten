package dev.nipponten.application.responses;

// trackingToken só existe nesta resposta: o banco guarda o hash, então ele não pode ser
// recuperado depois. É nulo quando o pedido é de um cliente com conta.
public record OrderCreatedResponse(OrderResponse order, String trackingToken) {}
