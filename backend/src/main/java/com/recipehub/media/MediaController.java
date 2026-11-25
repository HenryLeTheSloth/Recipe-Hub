package com.recipehub.media;

import com.recipehub.media.dto.MediaResponse;
import com.recipehub.user.AppUserDetails;
import java.time.Duration;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/media")
public class MediaController {

    private final MediaService media;
    private final StorageService storage;

    public MediaController(MediaService media, StorageService storage) {
        this.media = media;
        this.storage = storage;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MEDIA_UPLOAD')")
    @ResponseStatus(HttpStatus.CREATED)
    public MediaResponse upload(@AuthenticationPrincipal AppUserDetails principal, @RequestParam MultipartFile file) {
        return media.upload(principal.id(), file);
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> file(@PathVariable Long id) {
        Media m = media.get(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(m.getContentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)))
                .body(storage.load(m.getStorageKey()));
    }
}
