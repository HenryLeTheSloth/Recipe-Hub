package com.recipehub.media;

import com.recipehub.common.BaseEntity;
import jakarta.persistence.Entity;

@Entity
public class Media extends BaseEntity {

    private Long ownerId;
    private String storageKey;
    private String contentType;
    private long sizeBytes;

    protected Media() {
    }

    public Media(Long ownerId, String storageKey, String contentType, long sizeBytes) {
        this.ownerId = ownerId;
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }
}
