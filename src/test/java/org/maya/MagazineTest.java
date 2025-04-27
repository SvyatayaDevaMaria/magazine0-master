package org.maya;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.net.URL;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

@QuarkusTest
public class MagazineTest {

    @TestHTTPResource
    URL baseUrl;

    @Test
    public void testIndexEndpoint() {
        given()
                .when().get(baseUrl)
                .then()
                .statusCode(200)
                .body(containsString("Магазин"));
    }
}