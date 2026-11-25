package com.recipehub.user;

import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock FindByIndexNameSessionRepository<Session> repo;
    @Mock Session s1;
    @Mock Session s2;

    @AfterEach
    void clear() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void deletesImmediatelyOutsideTransaction() {
        when(repo.findByPrincipalName("a@x.com")).thenReturn(Map.of("s1", s1, "s2", s2));
        new SessionService(repo).invalidateOthers("a@x.com", "s2");
        verify(repo).deleteById("s1");
        verify(repo, never()).deleteById("s2");
    }

    // Xóa phiên chạy trong transaction riêng: nếu xóa trước khi transaction nghiệp vụ commit
    // thì user kịp đăng nhập lại với quyền cũ, hoặc mất phiên dù thay đổi bị rollback.
    @Test
    void defersDeleteUntilAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();
        when(repo.findByPrincipalName("a@x.com")).thenReturn(Map.of("s1", s1));
        new SessionService(repo).invalidateAll("a@x.com");
        verify(repo, never()).deleteById("s1");

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(repo).deleteById("s1");
    }
}
