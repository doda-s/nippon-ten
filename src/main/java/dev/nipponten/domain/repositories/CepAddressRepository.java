package dev.nipponten.domain.repositories;

import dev.nipponten.domain.models.CepAddress;

public interface CepAddressRepository {

    /** cep com só os 8 dígitos; nulo se não está no cache. */
    CepAddress getByCep(String cep);

    /** Insere ou substitui. */
    void save(CepAddress model);
}
