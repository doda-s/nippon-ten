package dev.nipponten.application.exceptions;

import java.util.List;

// Junta todos os itens com problema de uma vez, para o cliente ajustar o carrinho numa só ida.
public class UnavailableItemsException extends RuntimeException {

    private final List<Problem> problems;

    public UnavailableItemsException(List<Problem> problems) {
        super("Some items are unavailable");
        this.problems = List.copyOf(problems);
    }

    public List<Problem> getProblems() {
        return problems;
    }

    public record Problem(String field, String message) {}
}
