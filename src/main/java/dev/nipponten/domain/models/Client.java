package dev.nipponten.domain.models;

public record Client(
        Long id,
        Long userId,
        String name,
        String lastName,
        String cpf,
        String phone,
        Integer promotionPoints) {

    /** Nome e sobrenome, como a equipe chama o cliente. */
    public String fullName() {
        return lastName == null || lastName.isBlank() ? name : name + " " + lastName;
    }
}
