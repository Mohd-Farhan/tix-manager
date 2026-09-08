package com.support.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.support.entity.User;
import com.support.entity.UserRole;

/**
 * ==============================================================================================
 * REPOSITORY: UserRepository
 * ==============================================================================================
 * 
 * Provides fast indexed queries and pagination for user management.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    List<User> findByRole(UserRole role);

    Page<User> findByRole(UserRole role, Pageable pageable);

    @Query("SELECT u FROM User u")
    List<User> findAllIncludingDeleted();

    @Query("SELECT u FROM User u")
    Page<User> findAllIncludingDeleted(Pageable pageable);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.username = :username")
    boolean existsByUsernameIncludingDeleted(@Param("username") String username);
}