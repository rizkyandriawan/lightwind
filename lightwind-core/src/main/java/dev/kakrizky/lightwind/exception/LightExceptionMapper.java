package dev.kakrizky.lightwind.exception;

import dev.kakrizky.lightwind.filter.RequestIdFilter;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Provider
public class LightExceptionMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception e) {
        if (e instanceof WebApplicationException wae) {
            return wae.getResponse();
        }
        if (e instanceof ObjectNotFoundException) {
            return error(404, "Not Found", e.getMessage());
        }
        if (e instanceof BadRequestException) {
            return error(400, "Bad Request", e.getMessage());
        }
        if (e instanceof ValidationErrorException ve) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("timestamp", LocalDateTime.now().toString());
            body.put("status", 400);
            body.put("error", "Bad Request");
            body.put("message", ve.getMessage());
            body.put("errors", ve.getErrors());
            String rid = RequestIdFilter.getCurrentRequestId();
            if (rid != null) {
                body.put("requestId", rid);
            }
            return Response.status(400)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(body)
                    .build();
        }
        if (e instanceof UnauthorizedException) {
            return error(401, "Unauthorized", e.getMessage());
        }
        if (e instanceof ForbiddenException) {
            return error(403, "Forbidden", e.getMessage());
        }
        if (e instanceof ConflictException) {
            return error(409, "Conflict", e.getMessage());
        }
        if (e instanceof InternalServerException) {
            return error(500, "Internal Server Error", e.getMessage());
        }

        return error(500, "Internal Server Error", e.getMessage());
    }

    private Response error(int status, String error, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status);
        body.put("error", error);
        body.put("message", message);
        String requestId = RequestIdFilter.getCurrentRequestId();
        if (requestId != null) {
            body.put("requestId", requestId);
        }
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
