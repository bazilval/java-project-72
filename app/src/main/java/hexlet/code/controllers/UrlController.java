package hexlet.code.controllers;

import hexlet.code.domain.Url;
import hexlet.code.domain.UrlCheck;
import hexlet.code.domain.query.QUrl;
import io.ebean.DB;
import io.ebean.PagedList;
import io.ebean.Transaction;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;

import java.net.URL;
import java.util.List;
import java.util.stream.IntStream;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UrlController {
    private static final Logger LOGGER = LoggerFactory.getLogger("UrlController");

    public static Handler index = ctx -> {
        ctx.attribute("isIndex", true);
        ctx.render("index.html");
        LOGGER.info("INDEX PAGE IS RENDERED");
    };

    public static Handler createUrl = ctx -> {
        String name = ctx.formParam("url");
        String address;

        try {
            URL urlReader = new URL(name);
            String protocol = urlReader.getProtocol();
            String host = urlReader.getHost();
            String portString = urlReader.getPort() == -1 ? "" : ":" + String.valueOf(urlReader.getPort());
            address = protocol + "://" + host + portString;
        } catch (Exception e) {
            ctx.sessionAttribute("flash", "Некорректный URL");
            ctx.sessionAttribute("flash-type", "danger");
            ctx.status(HttpStatus.UNPROCESSABLE_CONTENT);
            ctx.render("index.html");
            LOGGER.error("INCORRECT URL");
            LOGGER.error(e.getMessage());
            return;
        }

        Url existingUrl = new QUrl()
                .name.ieq(address)
                .findOne();
        if (existingUrl != null) {
            ctx.sessionAttribute("flash", "Страница уже существует");
            ctx.sessionAttribute("flash-type", "success");

            ctx.redirect("/urls");
            LOGGER.info("URL ALREADY EXISTS");
            return;
        }

        Url url = new Url(address);
        url.save();

        ctx.sessionAttribute("flash", "Страница успешно добавлена");
        ctx.sessionAttribute("flash-type", "success");

        ctx.redirect("/urls");
        LOGGER.info("URL ADDED SUCCESSFULLY");
    };

    public static Handler listUrls = ctx -> {
        int urlsPerPage = 10;
        int normalizedPage;
        try {
            String page = ctx.queryParam("page");
            normalizedPage = Integer.parseInt(page);
        } catch (IllegalArgumentException e) {
            normalizedPage = 1;
        }

        int offset = (normalizedPage - 1) * urlsPerPage;

        PagedList<Url> pagedUrls = new QUrl()
                .setFirstRow(offset)
                .setMaxRows(urlsPerPage)
                .orderBy()
                .id.asc()
                .findPagedList();

        int pagesCount = pagedUrls.getTotalPageCount();

        int[] pages = IntStream.rangeClosed(1, pagesCount).toArray();
        List<Url> urls = pagedUrls.getList();

        ctx.attribute("urls", urls);
        ctx.attribute("currentPage", normalizedPage);
        ctx.attribute("pages", pages);

        ctx.render("urls/index.html");
        LOGGER.info("URLS PAGE IS RENDERED");
    };

    public static Handler showUrl = ctx -> {
        Long id = ctx.pathParamAsClass("id", Long.class).getOrDefault(null);

        Url url = new QUrl()
                .id.equalTo(id)
                .findOne();

        List<UrlCheck> checks = url.getChecks();

        ctx.attribute("url", url);
        ctx.attribute("checks", checks);
        ctx.render("urls/show.html");
        LOGGER.info("PAGE OF " + url.getName() + " IS RENDERED");
    };

    public static Handler addCheck = ctx -> {
        Long id = ctx.pathParamAsClass("id", Long.class).getOrDefault(null);
        String body = "";
        int statusCode = 0;

        Url url = new QUrl()
                .id.equalTo(id)
                .forUpdate()
                .findOne();

        try {
            HttpResponse<String> response = Unirest
                    .get(url.getName())
                    .asString();
            body = response.getBody();
            statusCode = response.getStatus();
        } catch (Exception e) {
            ctx.sessionAttribute("flash", "Некорректный адрес");
            ctx.sessionAttribute("flash-type", "danger");

            ctx.attribute("url", url);
            ctx.attribute("checks", url.getChecks());
            LOGGER.error(url.getName() + " IS INCORRECT ADDRESS");
            ctx.redirect("/urls/" + id);
            return;
        }

        Document document = Jsoup.parse(body);
        String title = getTagValue(document, "title");
        String h1 = getTagValue(document, "h1");
        String description = getDescription(document);

        UrlCheck check = new UrlCheck(statusCode, title, h1, description, url);

        try (Transaction transaction = DB.beginTransaction()) {
            url.addCheck(check);
            check.save();
            url.save();

            transaction.commit();
        }

        ctx.sessionAttribute("flash", "Страница успешно проверена");
        ctx.sessionAttribute("flash-type", "success");

        ctx.attribute("url", url);
        ctx.attribute("checks", url.getChecks());
        ctx.redirect("/urls/" + id);
        LOGGER.info("CHECK IS SUCCESSFUL");
    };

    private static String getTagValue(Document document, String tag) {
        Element element = document.selectFirst(tag);

        return element != null ? element.text() : "";
    }
    private static String getDescription(Document document) {
        Element element = document.selectFirst("meta[name=description]");

        return element != null ? element.attr("content") : "";
    }
}
