package dev.nipponten.resource.exception;

import dev.nipponten.application.exceptions.PaymentGatewayException;
import dev.nipponten.application.responses.ErrorResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

@Provider
public class PaymentGatewayExceptionMapper implements ExceptionMapper<PaymentGatewayException> {

    private static final Logger LOG = Logger.getLogger(PaymentGatewayExceptionMapper.class);

    // O detalhe do provedor vai para o log, não para o cliente.
    @Override
    public Response toResponse(PaymentGatewayException exception) {
        LOG.warn("Payment gateway failure", exception);
        return Response.status(Response.Status.BAD_GATEWAY)
                .entity(new ErrorResponse("Payment provider unavailable, try again"))
                .build();
    }
}
