package dev.nipponten.application.exceptions;

/**
 * O provedor de geocodificação falhou, recusou a chave ou estourou a cota. Não chega ao cliente: a
 * taxa de entrega vira 400, como quando o endereço não é encontrado (docs/delivery.md).
 */
public class GeocodingException extends RuntimeException {
    public GeocodingException(String message) {
        super(message);
    }

    public GeocodingException(String message, Throwable cause) {
        super(message, cause);
    }
}
