package hexlet.code;

import hexlet.code.domain.Url;
import hexlet.code.domain.query.QUrl;
import io.ebean.DB;
import io.javalin.Javalin;
import io.ebean.Database;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


class AppTest {

    private static Javalin app;
    private static String baseUrl;

    @BeforeAll
    static void beforeAll() {
        app = App.getApp();
        app.start(0);
        int port = app.port();
        baseUrl = "http://localhost:" + port;
    }
    @AfterAll
    static void afterAll() {
        app.stop();
    }
    @BeforeEach
    void beforeEach() {
        Database db = DB.getDefault();
        db.truncate("url");
        Url existingUrl = new Url("http://hexlet.io");
        existingUrl.save();
    }

    @Test
    void testIndex() {
        HttpResponse<String> response = Unirest
                .get(baseUrl + "/")
                .asString();

        assertEquals(response.getStatus(), 200);
    }
    @Test
    void testUrls() {
        HttpResponse<String> response = Unirest
                .get(baseUrl + "/urls")
                .asString();

        assertTrue(response.getBody().contains("http://hexlet.io"));
        assertEquals(response.getStatus(), 200);
    }
    @Test
    void testCorrectCreateUrl() {
        String name = "http://meduza.io";

        HttpResponse<String> response = Unirest
                .post(baseUrl + "/urls")
                .field("name", name)
                .asString();

        assertEquals(response.getStatus(), 302);

        Url url = new QUrl()
                .name.ieq("http://meduza.io")
                .findOne();

        assertNotNull(url);
    }
    @Test
    void testIncorrectCreateUrl() {
        String name = "meduza";

        HttpResponse<String> response = Unirest
                .post(baseUrl + "/urls")
                .field("name", name)
                .asString();

        String body = response.getBody();

        Url url = new QUrl()
                .name.ieq("meduza")
                .findOne();

        assertNull(url);
        assertEquals(response.getStatus(), 422);
    }

    @Test
    void testShowUrl() {
        Url url = new QUrl()
                .name.ieq("http://hexlet.io")
                .findOne();

        Long id = url.getId();

        HttpResponse<String> response = Unirest
                .get(baseUrl + "/urls/" + id)
                .asString();

        assertEquals(response.getStatus(), 200);
        assertTrue(response.getBody().contains("http://hexlet.io"));
    }
}
