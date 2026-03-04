package dev.kakrizky.lightwind.exception;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LightExceptionMapperTest {

    private final LightExceptionMapper mapper = new LightExceptionMapper();

    @Test
    void maps_objectNotFoundException_to404() {
        Response response = mapper.toResponse(new ObjectNotFoundException("Not found"));
        assertEquals(404, response.getStatus());
    }

    @Test
    void maps_badRequestException_to400() {
        Response response = mapper.toResponse(new BadRequestException("Bad request"));
        assertEquals(400, response.getStatus());
    }

    @Test
    void maps_unauthorizedException_to401() {
        Response response = mapper.toResponse(new UnauthorizedException("Unauthorized"));
        assertEquals(401, response.getStatus());
    }

    @Test
    void maps_forbiddenException_to403() {
        Response response = mapper.toResponse(new ForbiddenException("Forbidden"));
        assertEquals(403, response.getStatus());
    }

    @Test
    void maps_conflictException_to409() {
        Response response = mapper.toResponse(new ConflictException("Conflict"));
        assertEquals(409, response.getStatus());
    }

    @Test
    void maps_internalServerException_to500() {
        Response response = mapper.toResponse(new InternalServerException("Error"));
        assertEquals(500, response.getStatus());
    }

    @Test
    void maps_validationErrorException_to400_withErrors() {
        var errors = List.of(new ValidationError("name", "Required"));
        Response response = mapper.toResponse(new ValidationErrorException("Validation failed", errors));
        assertEquals(400, response.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getEntity();
        assertEquals("Validation failed", body.get("message"));
        assertNotNull(body.get("errors"));
    }

    @Test
    void maps_genericException_to500() {
        Response response = mapper.toResponse(new RuntimeException("Something broke"));
        assertEquals(500, response.getStatus());
    }
}
