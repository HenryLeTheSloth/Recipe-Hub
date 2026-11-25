package com.recipehub.user;

import java.util.List;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class SessionService {

    private final FindByIndexNameSessionRepository<? extends Session> sessions;

    public SessionService(FindByIndexNameSessionRepository<? extends Session> sessions) {
        this.sessions = sessions;
    }

    /** Xóa mọi phiên đăng nhập của user (principal name = email). */
    public void invalidateAll(String email) {
        invalidateOthers(email, null);
    }

    /** Như invalidateAll nhưng giữ lại phiên keepId (phiên đang thao tác). */
    public void invalidateOthers(String email, String keepId) {
        List<String> ids = sessions.findByPrincipalName(email).keySet().stream()
                .filter(id -> !id.equals(keepId)).toList();
        // Xóa sau commit: trước đó user có thể đăng nhập lại kịp với quyền cũ, hoặc mất phiên dù rollback.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    ids.forEach(sessions::deleteById);
                }
            });
        } else {
            ids.forEach(sessions::deleteById);
        }
    }
}
