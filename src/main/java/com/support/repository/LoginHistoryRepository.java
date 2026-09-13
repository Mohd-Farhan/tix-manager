package com.support.repository;

import com.support.entity.LoginHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {

    Optional<LoginHistory> findFirstByUsernameAndLogoutTimeIsNullOrderByLoginTimeDesc(String username);

    Page<LoginHistory> findByUsernameOrderByLoginTimeDesc(String username, Pageable pageable);

    Page<LoginHistory> findAllByOrderByLoginTimeDesc(Pageable pageable);
}
