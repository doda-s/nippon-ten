package dev.nipponten.application.exceptions;

public class InternalRoleNotFoundException extends NotFoundException {
    public InternalRoleNotFoundException(Long id) {
        super("InternalRole not found: " + id);
    }
}
