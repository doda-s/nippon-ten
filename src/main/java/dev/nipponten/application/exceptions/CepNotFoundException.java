package dev.nipponten.application.exceptions;

public class CepNotFoundException extends NotFoundException {
    public CepNotFoundException(String cep) {
        super("CEP not found: " + cep);
    }
}
