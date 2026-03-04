package dev.kakrizky.lightwind.integration;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CrudIntegrationTest {

    private static UUID createdId;

    @Test
    @Order(1)
    void create_returnsCreatedItem() {
        createdId = UUID.fromString(
            given()
                .contentType(ContentType.JSON)
                .body("""
                    {"name": "Widget A", "description": "A test widget", "price": 100, "status": "active", "category": "electronics"}
                """)
            .when()
                .post("/api/test-items")
            .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.name", equalTo("Widget A"))
                .body("data.price", equalTo(100))
                .body("data.id", notNullValue())
                .extract().path("data.id")
        );
    }

    @Test
    @Order(2)
    void create_secondItem() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {"name": "Widget B", "description": "Another widget", "price": 250, "status": "inactive", "category": "tools"}
            """)
        .when()
            .post("/api/test-items")
        .then()
            .statusCode(200)
            .body("data.name", equalTo("Widget B"));
    }

    @Test
    @Order(3)
    void create_thirdItem() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {"name": "Gadget C", "description": "A gadget", "price": 50, "status": "active", "category": "electronics"}
            """)
        .when()
            .post("/api/test-items")
        .then()
            .statusCode(200);
    }

    @Test
    @Order(4)
    void getAll_returnsPaginatedList() {
        given()
        .when()
            .get("/api/test-items")
        .then()
            .statusCode(200)
            .body("code", equalTo(200))
            .body("data.items", hasSize(3))
            .body("data.meta.totalData", equalTo(3))
            .body("data.meta.currentPage", equalTo(1));
    }

    @Test
    @Order(5)
    void getAll_withPagination() {
        given()
            .queryParam("page", 1)
            .queryParam("size", 2)
        .when()
            .get("/api/test-items")
        .then()
            .statusCode(200)
            .body("data.items", hasSize(2))
            .body("data.meta.totalData", equalTo(3))
            .body("data.meta.totalPages", equalTo(2));
    }

    @Test
    @Order(6)
    void getAll_withExactFilter() {
        given()
            .queryParam("status", "active")
        .when()
            .get("/api/test-items")
        .then()
            .statusCode(200)
            .body("data.items", hasSize(2))
            .body("data.items.status", everyItem(equalTo("active")));
    }

    @Test
    @Order(7)
    void getAll_withGteFilter() {
        given()
            .queryParam("price__gte", "100")
        .when()
            .get("/api/test-items")
        .then()
            .statusCode(200)
            .body("data.items", hasSize(2));
    }

    @Test
    @Order(8)
    void getAll_withSearch() {
        given()
            .queryParam("search", "gadget")
        .when()
            .get("/api/test-items")
        .then()
            .statusCode(200)
            .body("data.items", hasSize(1))
            .body("data.items[0].name", equalTo("Gadget C"));
    }

    @Test
    @Order(9)
    void getAll_withSortAsc() {
        given()
            .queryParam("sort", "price,asc")
        .when()
            .get("/api/test-items")
        .then()
            .statusCode(200)
            .body("data.items[0].price", equalTo(50))
            .body("data.items[2].price", equalTo(250));
    }

    @Test
    @Order(10)
    void getOne_returnsItem() {
        given()
        .when()
            .get("/api/test-items/" + createdId)
        .then()
            .statusCode(200)
            .body("data.name", equalTo("Widget A"))
            .body("data.id", equalTo(createdId.toString()));
    }

    @Test
    @Order(11)
    void update_modifiesItem() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {"name": "Widget A Updated", "price": 150, "status": "active", "category": "electronics"}
            """)
        .when()
            .put("/api/test-items/" + createdId)
        .then()
            .statusCode(200)
            .body("data.name", equalTo("Widget A Updated"))
            .body("data.price", equalTo(150));
    }

    @Test
    @Order(12)
    void delete_softDeletesItem() {
        given()
        .when()
            .delete("/api/test-items/" + createdId)
        .then()
            .statusCode(200)
            .body("data.name", equalTo("Widget A Updated"));
    }

    @Test
    @Order(13)
    void getAll_excludesSoftDeleted() {
        given()
        .when()
            .get("/api/test-items")
        .then()
            .statusCode(200)
            .body("data.items", hasSize(2))
            .body("data.meta.totalData", equalTo(2));
    }

    @Test
    @Order(14)
    void getOne_returns404_forSoftDeleted() {
        given()
        .when()
            .get("/api/test-items/" + createdId)
        .then()
            .statusCode(404);
    }

    @Test
    @Order(15)
    void restore_bringBackItem() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .post("/api/test-items/" + createdId + "/restore")
        .then()
            .statusCode(200)
            .body("data.name", equalTo("Widget A Updated"));
    }

    @Test
    @Order(16)
    void getAll_afterRestore_showsItem() {
        given()
        .when()
            .get("/api/test-items")
        .then()
            .statusCode(200)
            .body("data.items", hasSize(3));
    }

    @Test
    @Order(17)
    void create_withMissingName_returnsValidationError() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {"description": "No name", "price": 100}
            """)
        .when()
            .post("/api/test-items")
        .then()
            .statusCode(400)
            .body("message", equalTo("Validation failed"))
            .body("errors", hasSize(1))
            .body("errors[0].field", equalTo("name"));
    }

    @Test
    @Order(18)
    void getOne_returns404_forNonexistent() {
        given()
        .when()
            .get("/api/test-items/" + UUID.randomUUID())
        .then()
            .statusCode(404);
    }
}
