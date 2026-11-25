package dev.nipponten.infrastructure.geo;

import dev.nipponten.application.exceptions.AddressLookupException;
import dev.nipponten.domain.gateways.CepLookup;
import dev.nipponten.domain.models.CepAddress;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

/**
 * Consulta o CEP no ViaCEP e, se ele falhar ou não conhecer o CEP, na BrasilAPI. As duas são
 * gratuitas e sem chave, e as bases divergem um pouco: um CEP novo pode existir só em uma delas.
 */
@ApplicationScoped
public class PublicCepLookup implements CepLookup {

    private static final Logger LOG = Logger.getLogger(PublicCepLookup.class);

    @Inject @RestClient ViaCepClient viaCep;

    @Inject @RestClient BrasilApiCepClient brasilApi;

    @Inject Clock clock;

    @Override
    public Optional<CepAddress> find(String cep) {
        RuntimeException viaCepFailure = null;
        try {
            ViaCepClient.Response response = viaCep.get(cep);
            if (!response.notFound()) {
                return Optional.of(
                        address(
                                cep,
                                response.logradouro(),
                                response.bairro(),
                                response.localidade(),
                                response.uf()));
            }
        } catch (WebApplicationException | ProcessingException e) {
            LOG.warnf("ViaCEP failed for %s, trying BrasilAPI: %s", cep, e.getMessage());
            viaCepFailure = e;
        }
        try {
            BrasilApiCepClient.Response response = brasilApi.get(cep);
            return Optional.of(
                    address(
                            cep,
                            response.street(),
                            response.neighborhood(),
                            response.city(),
                            response.state()));
        } catch (WebApplicationException e) {
            if (e.getResponse().getStatus() == 404) {
                return Optional.empty();
            }
            return failed(cep, viaCepFailure, e);
        } catch (ProcessingException e) {
            return failed(cep, viaCepFailure, e);
        }
    }

    // Se o ViaCEP respondeu que o CEP não existe, a queda da reserva não muda essa resposta.
    private static Optional<CepAddress> failed(
            String cep, RuntimeException viaCepFailure, RuntimeException brasilApiFailure) {
        if (viaCepFailure == null) {
            return Optional.empty();
        }
        brasilApiFailure.addSuppressed(viaCepFailure);
        throw new AddressLookupException("No CEP provider answered for " + cep, brasilApiFailure);
    }

    private CepAddress address(
            String cep, String street, String neighborhood, String city, String state) {
        return new CepAddress(
                cep,
                blankToNull(street),
                blankToNull(neighborhood),
                city,
                state,
                Instant.now(clock));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
