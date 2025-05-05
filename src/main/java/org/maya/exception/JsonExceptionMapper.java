package org.maya.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

@Provider
public class JsonExceptionMapper implements ExceptionMapper<JsonProcessingException> {

    @Inject
    Logger logger;

    @Override
    public Response toResponse(JsonProcessingException exception) {
        logger.error("Invalid JSON format", exception);
        return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\":\"Неверный JSON формат\", \"details\":\"" + exception.getMessage() + "\"}")
                .build();
    }
}