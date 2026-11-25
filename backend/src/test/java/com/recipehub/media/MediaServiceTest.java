package com.recipehub.media;

import com.recipehub.common.ApiException;
import com.recipehub.common.ErrorCode;
import java.io.InputStream;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    static final byte[] PNG = MediaTypesTest.hex("89504E470D0A1A0A0000000D49484452");

    @Mock MediaRepository repo;
    @Mock StorageService storage;
    MediaService service;

    @BeforeEach
    void setUp() {
        service = new MediaService(repo, storage);
        lenient().when(repo.save(any())).thenAnswer(i -> {
            Media m = i.getArgument(0);
            ReflectionTestUtils.setField(m, "id", 9L);
            return m;
        });
    }

    static void assertCode(ThrowingCallable call, ErrorCode code) {
        assertThatThrownBy(call).isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code()).isEqualTo(code);
    }

    @Test
    void pngNamedJpgIsStoredAsPng() throws Exception {
        when(storage.store(any(), eq("png"))).thenReturn("k.png");
        var res = service.upload(3L, new MockMultipartFile("file", "a.jpg", "image/jpeg", PNG));
        assertThat(res.url()).isEqualTo("/api/media/9/file");
        var saved = ArgumentCaptor.forClass(Media.class);
        verify(repo).save(saved.capture());
        assertThat(saved.getValue().getContentType()).isEqualTo("image/png");
        assertThat(saved.getValue().getStorageKey()).isEqualTo("k.png");
        assertThat(saved.getValue().getOwnerId()).isEqualTo(3L);
        assertThat(saved.getValue().getSizeBytes()).isEqualTo(PNG.length);
        var stream = ArgumentCaptor.forClass(InputStream.class);
        verify(storage).store(stream.capture(), eq("png"));
    }

    @Test
    void storesWholeFileIncludingHeader() throws Exception {
        byte[][] written = new byte[1][];
        when(storage.store(any(), eq("png"))).thenAnswer(i -> {
            written[0] = ((InputStream) i.getArgument(0)).readAllBytes();
            return "k.png";
        });
        service.upload(3L, new MockMultipartFile("file", "a.png", "image/png", PNG));
        assertThat(written[0]).containsExactly(PNG);
    }

    @Test
    void htmlNamedJpgIsRejected() {
        assertCode(() -> service.upload(3L, new MockMultipartFile("file", "a.jpg", "image/jpeg",
                "<html><script>alert(1)</script>".getBytes())), ErrorCode.MEDIA_TYPE);
        verifyNoInteractions(storage);
    }

    @Test
    void emptyFileIsRejected() {
        assertCode(() -> service.upload(3L, new MockMultipartFile("file", "a.png", "image/png", new byte[0])),
                ErrorCode.MEDIA_TYPE);
        verifyNoInteractions(storage);
    }

    @Test
    void overFiveMegabytesRejected() {
        var file = mock(org.springframework.web.multipart.MultipartFile.class);
        when(file.getSize()).thenReturn(5L * 1024 * 1024 + 1);
        assertCode(() -> service.upload(3L, file), ErrorCode.MEDIA_TOO_LARGE);
        verifyNoInteractions(storage);
    }

    @Test
    void requireOwnedRejectsOthersMedia() {
        Media m = new Media(5L, "k.png", "image/png", 10);
        when(repo.findById(1L)).thenReturn(Optional.of(m));
        assertCode(() -> service.requireOwned(1L, 3L), ErrorCode.FORBIDDEN);
        assertThat(service.requireOwned(1L, 5L)).isSameAs(m);
    }

    @Test
    void requireOwnedUnknownIsNotFound() {
        when(repo.findById(1L)).thenReturn(Optional.empty());
        assertCode(() -> service.requireOwned(1L, 3L), ErrorCode.NOT_FOUND);
    }

    @Test
    void urlIsNullSafe() {
        assertThat(MediaService.url(null)).isNull();
    }
}
