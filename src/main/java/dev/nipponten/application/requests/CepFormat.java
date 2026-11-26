package dev.nipponten.application.requests;

/** Formato aceito para o CEP nos requests: 8 dígitos, com ou sem hífen. */
public final class CepFormat {

    public static final String REGEXP = "\\d{5}-?\\d{3}";

    public static final String MESSAGE = "must be a CEP like 01310-100";

    private CepFormat() {}
}
