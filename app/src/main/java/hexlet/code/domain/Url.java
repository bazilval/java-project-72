package hexlet.code.domain;

import io.ebean.Model;
import io.ebean.annotation.WhenCreated;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Entity
public final class Url extends Model {
    @Id
    private Long id;
    private String name;
    @WhenCreated
    private Instant createdAt;
    @OneToMany(mappedBy = "url", cascade = CascadeType.ALL, orphanRemoval = true)
    List<UrlCheck> urlChecks;

    public Url(String name) {
        this.name = name;
        this.urlChecks = new ArrayList<>();
    }
    public Long getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public Instant getCreatedAt() {
        return createdAt;
    }
    public List<UrlCheck> getChecks() {
        return urlChecks;
    }
    public void addCheck(UrlCheck check) {
        urlChecks.add(check);
    }
    public UrlCheck getLastCheck() {
        return urlChecks.stream()
                .sorted(Comparator.comparing(UrlCheck::getCreatedAt).reversed())
                .findFirst()
                .orElse(null);
    }
}
