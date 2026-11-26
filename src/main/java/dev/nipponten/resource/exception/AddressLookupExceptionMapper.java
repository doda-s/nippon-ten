package dev.nipponten.resource.exception;

import dev.nipponten.application.exceptions.AddressLookupException;
import dev.nipponten.application.responses.ErrorResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

@Provider
public class AddressLookupExceptionMapper implements ExceptionMapper<AddressLookupException> {

    private static final Logger LOG = Logger.getLogger(AddressLookupExceptionMapper.class);

    // O detalhe do provedor vai para o log, não para o cliente.
    @Override
    public Response toResponse(AddressLookupException exception) {
        LOG.warn("CEP lookup failure", exception);
        return Response.status(Response.Status.BAD_GATEWAY)
                .entity(new ErrorResponse("Address lookup unavailable, try again"))
                .build();
    }
}
