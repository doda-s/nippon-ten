package dev.nipponten.application.requests;

/**
 * Formato extra para {@code @Email}. O {@code @Email} do Bean Validation aceita domínio sem ponto
 * ({@code ana@gmailcom}), que o Mercado Pago recusa no pagador; com este padrão, o erro sai como
 * {@code 400} antes da cobrança, e não como {@code 502} depois dela.
 */
public final class EmailFormat {

    /** Algo antes do {@code @}, e um domínio com pelo menos um ponto, sem ponto nas pontas. */
    public static final String WITH_DOTTED_DOMAIN = "[^@\\s]+@[^@\\s.]+(\\.[^@\\s.]+)+";

    private EmailFormat() {}
}
