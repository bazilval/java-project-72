package hexlet.code;

import hexlet.code.domain.Url;
import hexlet.code.domain.UrlCheck;
import hexlet.code.domain.query.QUrl;
import hexlet.code.domain.query.QUrlCheck;
import io.ebean.DB;
import io.javalin.Javalin;
import io.ebean.Database;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
    void beforeEach() throws IOException {
        Database db = DB.getDefault();

        Path deleteSQLFile = Path.of("src/test/resources/deleteAll.sql");
        String deleteSQL = Files.readString(deleteSQLFile);
        db.sqlUpdate(deleteSQL).executeNow();

        Url existingUrl = new Url("http://hexlet.io");
        UrlCheck check = new UrlCheck(200,
                "Hexlet",
                "Many of our graduates are sought after by companies",
                "Live online community of programmers and developers.",
                existingUrl);
        existingUrl.addCheck(check);
        existingUrl.save();
        check.save();
    }

    @Test
    void testIndex() {
        HttpResponse<String> response = Unirest
                .get(baseUrl + "/")
                .asString();

        assertEquals(200, response.getStatus());
    }
    @Test
    void testUrls() {
        HttpResponse<String> response = Unirest
                .get(baseUrl + "/urls")
                .asString();

        assertTrue(response.getBody().contains("http://hexlet.io"));
        assertEquals(200, response.getStatus());
    }
    @Test
    void testCorrectCreateUrl() {
        String name = "http://meduza.io";

        HttpResponse<String> response = Unirest
                .post(baseUrl + "/urls")
                .field("url", name)
                .asString();

        assertEquals(302, response.getStatus());

        Url url = new QUrl()
                .name.ieq("http://meduza.io")
                .findOne();

        assertNotNull(url);
    }
    @Test
    void testCreateExistingUrl() {
        String name = "http://hexlet.io";

        HttpResponse<String> response = Unirest
                .post(baseUrl + "/urls")
                .field("url", name)
                .asString();

        Integer urlCount = new QUrl()
                .name.ieq("http://hexlet.io")
                .findList()
                .size();

        assertEquals(1, urlCount);
    }
    @Test
    void testIncorrectCreateUrl() {
        String name = "meduza";

        HttpResponse<String> response = Unirest
                .post(baseUrl + "/urls")
                .field("url", name)
                .asString();

        String body = response.getBody();

        Url url = new QUrl()
                .name.ieq("meduza")
                .findOne();

        assertNull(url);
        assertEquals(422, response.getStatus());
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

        assertEquals(200, response.getStatus());
        assertTrue(response.getBody().contains("http://hexlet.io"));
        assertTrue(response.getBody().contains("Hexlet"));
        assertTrue(response.getBody().contains("Many of our gradu..."));
    }
    @Test
    void testCheck() throws IOException {
        MockWebServer server = new MockWebServer();
        Path file = Path.of("src/test/resources/body.html");
        String body = Files.readString(file);
        server.enqueue(new MockResponse().setBody(body));
        server.start();

        HttpUrl name = server.url("/");
        String urlString = name.scheme() + "://" + name.host() + ":" + name.port();

        Url newUrl = new Url(urlString);
        newUrl.save();
        Long id = newUrl.getId();

        HttpResponse<String> responce = Unirest.post(baseUrl + "/urls/" + id + "/checks").asString();

        List<UrlCheck> checks = new QUrlCheck()
                .findList();
        var check = checks.get(1);

        assertEquals("Work at Meduza! We are looking for a social media editor", check.getH1());
        assertEquals("News - Meduza", check.getTitle());
        assertEquals("Nothing extra, just the facts", check.getDescription());
    }
}
