package com.support.repository;

import com.support.entity.LoginHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {

    Optional<LoginHistory> findFirstByUsernameAndLogoutTimeIsNullOrderByLoginTimeDesc(String username);

    Page<LoginHistory> findByUsernameOrderByLoginTimeDesc(String username, Pageable pageable);

    Page<LoginHistory> findAllByOrderByLoginTimeDesc(Pageable pageable);

    @Query("SELECT l.id FROM LoginHistory l WHERE l.loginTime < :cutoff ORDER BY l.loginTime ASC")
    List<Long> findOldLoginHistoryIds(@Param("cutoff") LocalDateTime cutoff, Pageable pageable);

    @Modifying
    @Query("DELETE FROM LoginHistory l WHERE l.id IN :ids")
    int deleteBatchByIds(@Param("ids") List<Long> ids);
}
