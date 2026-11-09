package dev.nipponten.domain.models;

/** Saldo disponível e o próximo lote a vencer (nulo se nenhum vence). */
public record PointsBalance(int balance, PointsTransaction nextExpiring) {}
