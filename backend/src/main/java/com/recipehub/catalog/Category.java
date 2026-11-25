package com.recipehub.catalog;

import com.recipehub.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "categories")
public class Category extends BaseEntity {

    private String name;
    private String slug;
    private int sortOrder;

    protected Category() {
    }

    public Category(String name, String slug, int sortOrder) {
        this.name = name;
        this.slug = slug;
        this.sortOrder = sortOrder;
    }

    public String getName() { return name; }
    public String getSlug() { return slug; }
    public int getSortOrder() { return sortOrder; }
    public void setName(String name) { this.name = name; }
    public void setSlug(String slug) { this.slug = slug; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
