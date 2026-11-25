package com.recipehub.user;

import com.recipehub.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "permissions")
public class Permission extends BaseEntity {

    private String code;
    private String description;

    protected Permission() {
    }

    Permission(String code, String description) { // quyền do migration tạo; constructor này cho test
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
