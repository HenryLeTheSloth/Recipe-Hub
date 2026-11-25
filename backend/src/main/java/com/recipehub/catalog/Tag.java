package com.recipehub.catalog;

import com.recipehub.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "tags")
public class Tag extends BaseEntity {

    private String name;
    private String slug;

    protected Tag() {
    }

    public Tag(String name, String slug) {
        this.name = name;
        this.slug = slug;
    }

    public String getName() { return name; }
    public String getSlug() { return slug; }
    public void setName(String name) { this.name = name; }
    public void setSlug(String slug) { this.slug = slug; }
}
