package dev.nipponten.application.exceptions;

/**
 * Um provedor de endereço não respondeu: nenhum provedor de CEP, com o CEP fora do cache, ou o
 * geocoder ao configurar a loja. A API responde 502.
 */
public class AddressLookupException extends RuntimeException {
    public AddressLookupException(String message, Throwable cause) {
        super(message, cause);
    }
}
