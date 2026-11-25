package dev.nipponten.domain.gateways;

import dev.nipponten.domain.models.Address;
import dev.nipponten.domain.models.Coordinates;
import java.util.Optional;

/**
 * Porta para transformar endereço em coordenada. Recebe o endereço já com cidade e UF. Vazio = o
 * provedor não achou o endereço com precisão de rua; falha de comunicação lança {@code
 * GeocodingException}.
 */
public interface Geocoder {

    Optional<Coordinates> geocode(Address address);
}
