package dev.nipponten.resource;

import dev.nipponten.application.requests.CepFormat;
import dev.nipponten.application.responses.CepAddressResponse;
import dev.nipponten.application.services.CepService;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

// Pública: o formulário de endereço do visitante também se completa pelo CEP.
@Path("/addresses")
@Produces(MediaType.APPLICATION_JSON)
public class AddressResource {

    @Inject CepService cepService;

    @GET
    @PermitAll
    @Path("/cep/{cep}")
    public CepAddressResponse getByCep(
            @PathParam("cep") @Pattern(regexp = CepFormat.REGEXP, message = CepFormat.MESSAGE)
                    String cep) {
        return CepAddressResponse.of(cepService.getByCep(cep));
    }
}
