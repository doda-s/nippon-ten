package dev.nipponten.application.exceptions;

/** Nenhum provedor de CEP respondeu e o CEP não está no cache; a API responde 502. */
public class AddressLookupException extends RuntimeException {
    public AddressLookupException(String message, Throwable cause) {
        super(message, cause);
    }
}
