package dev.nipponten.infrastructure.geo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

// Regra pura do GoogleGeocoder, sem Quarkus: quando o resultado parcial do Google ainda é a rua
// digitada.
class StreetNameMatchTest {

    @Test
    void acceptsTheSameStreetWrittenAnotherWay() {
        assertTrue(
                GoogleGeocoder.sameStreetName(
                        "Av. Dr. Carlos Botelho", "Avenida Doutor Carlos Botelho"));
        assertTrue(GoogleGeocoder.sameStreetName("rua ceara", "Rua Ceará"));
        assertTrue(GoogleGeocoder.sameStreetName("Paulista", "Avenida Paulista"));
        assertTrue(GoogleGeocoder.sameStreetName("Carlos Botelho", "Av. Dr. Carlos Botelho"));
        assertTrue(GoogleGeocoder.sameStreetName("R. 7 de Setembro", "Rua 7 de Setembro"));
    }

    @Test
    void refusesAnotherStreet() {
        assertFalse(GoogleGeocoder.sameStreetName("Rua Qwxyzk Inexistente", "Rua Ceará"));
        assertFalse(GoogleGeocoder.sameStreetName("Rua A", "Rua Ana"));
        assertFalse(GoogleGeocoder.sameStreetName("Rua Episcopa", "Rua Episcopal"));
        assertFalse(GoogleGeocoder.sameStreetName("", "Rua Ceará"));
    }
}
