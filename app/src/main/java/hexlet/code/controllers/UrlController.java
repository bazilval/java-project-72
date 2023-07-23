package hexlet.code.controllers;

import hexlet.code.domain.Url;
import hexlet.code.domain.query.QUrl;
import io.ebean.PagedList;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;

import java.net.URL;
import java.util.List;
import java.util.stream.IntStream;

public class UrlController {
    public static Handler index = ctx -> {
        ctx.attribute("isIndex", true);
        ctx.render("index.html");
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
            return;
        }

        Url url = new Url(address);
        url.save();

        ctx.sessionAttribute("flash", "Страница успешно добавлена");
        ctx.sessionAttribute("flash-type", "success");

        ctx.redirect("/urls");
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
    };
    public static Handler showUrl = ctx -> {
        Long id = ctx.pathParamAsClass("id", Long.class).getOrDefault(null);

        Url url = new QUrl()
                .id.equalTo(id)
                .findOne();

        List checks = List.of();

        ctx.attribute("url", url);
        ctx.attribute("checks", checks);
        ctx.render("urls/show.html");
    };
}
