package dev.nipponten.resource.security;

/**
 * Nomes das roles da identidade, em constantes para as anotações de segurança (anotação não aceita
 * {@code Enum.name()}). As de permissão espelham {@code InternalPermission}; um teste garante que
 * batem.
 */
public final class Permissions {

    public static final String MANAGE_USERS = "MANAGE_USERS";
    public static final String MANAGE_ROLES = "MANAGE_ROLES";
    public static final String MANAGE_PRODUCTS = "MANAGE_PRODUCTS";
    public static final String MANAGE_INGREDIENTS = "MANAGE_INGREDIENTS";
    public static final String MANAGE_PROMOTIONS = "MANAGE_PROMOTIONS";

    /** Interno com a role reservada; ver {@code SuperAdminService}. */
    public static final String SUPER_ADMIN = "SUPER_ADMIN";

    private Permissions() {}
}
