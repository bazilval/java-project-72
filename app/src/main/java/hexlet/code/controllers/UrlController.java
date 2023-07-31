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
        String name = ctx.formParam("name");
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
        String title = getTagValue(body, "title");
        String h1 = getTagValue(body, "h1");
        String description = getDescription(body);

        UrlCheck check = new UrlCheck(statusCode, title, h1, description, url);

        try (Transaction transaction = DB.beginTransaction()) {
            url.addCheck(check);
            check.save();
            url.save();

            transaction.commit();
            //LOGGER.info("TRANSACTION SUCCESSFUL");
        }

        ctx.sessionAttribute("flash", "Страница успешно проверена");
        ctx.sessionAttribute("flash-type", "success");

        ctx.attribute("url", url);
        ctx.attribute("checks", url.getChecks());
        ctx.redirect("/urls/" + id);
        LOGGER.info("CHECK IS SUCCESSFUL");
    };

    private static String getTagValue(String body, String tag) {
        int indexOfEnd = body.indexOf("</" + tag + ">");

        if (indexOfEnd == -1) {
            return "";
        }

        String text = body.substring(0, indexOfEnd);
        int indexOfBegin = text.lastIndexOf(">");

        return text.substring(indexOfBegin + 1);
    }
    private static String getDescription(String body) {
        String beginningTag = "name=\"description\"";
        int indexOfBegin = body.indexOf(beginningTag);

        if (indexOfBegin == -1) {
            return "";
        }

        String text = body.substring(indexOfBegin);
        String contentAtt = "content=";
        indexOfBegin = text.indexOf(contentAtt);
        text = text.substring(indexOfBegin + contentAtt.length() + 1);

        int indexOfEnd = text.indexOf(">");

        return text.substring(0, indexOfEnd - 2);
    }
}
