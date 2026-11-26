package dev.nipponten.domain.gateways;

import dev.nipponten.domain.models.CepAddress;
import java.util.Optional;

/**
 * Porta para a consulta de CEP. Vazio = o CEP não existe. Quando nenhum provedor responde, lança
 * {@code AddressLookupException}.
 */
public interface CepLookup {

    /** cep com só os 8 dígitos. */
    Optional<CepAddress> find(String cep);
}
