package hexlet.code.domain;

import io.ebean.Model;
import io.ebean.annotation.ConstraintMode;
import io.ebean.annotation.DbForeignKey;
import io.ebean.annotation.WhenCreated;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.JoinColumn;
import java.time.Instant;

@Entity
public final class UrlCheck extends Model {
    @Id
    private Long id;
    @WhenCreated
    private Instant createdAt;
    private int statusCode;
    private String h1;
    private String title;

    @Lob
    private String description;
    @ManyToOne(optional = false)
    @JoinColumn(name = "url_id", referencedColumnName = "id")
    @DbForeignKey (onDelete = ConstraintMode.CASCADE)
    private Url url;

    public UrlCheck(int statusCode, String title, String h1, String description, Url url) {
        this.statusCode = statusCode;
        this.title = title;
        this.h1 = h1;
        this.description = description;
        this.url = url;
    }

    public Long getId() {
        return id;
    }
    public Instant getCreatedAt() {
        return createdAt;
    }
    public int getStatusCode() {
        return statusCode;
    }
    public String getTitle() {
        return title;
    }
    public String getH1() {
        return h1;
    }
    public String getDescription() {
        return description;
    }
    public Url getUrl() {
        return url;
    }
}
