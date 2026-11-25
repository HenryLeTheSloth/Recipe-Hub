package com.recipehub.user;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByCode(String code);

    List<Role> findAllByCodeIn(Collection<String> codes);

    /** Khóa hàng role ADMIN tới hết transaction: tuần tự hóa các thay đổi có thể làm mất admin cuối cùng. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Role r where r.code = 'ADMIN'")
    Optional<Role> lockAdminRole();
}
