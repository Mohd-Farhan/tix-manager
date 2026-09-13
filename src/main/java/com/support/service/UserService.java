package com.support.service;

import com.support.dto.BulkUploadResultDTO;
import com.support.dto.CreateUserRequest;
import com.support.dto.PasswordChangeDTO;
import com.support.dto.UserDTO;
import com.support.entity.User;
import com.support.entity.UserRole;
import com.support.exception.DuplicateResourceException;
import com.support.exception.InvalidOperationException;
import com.support.exception.ResourceNotFoundException;
import com.support.mapper.UserMapper;
import com.support.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ==============================================================================================
 * SERVICE: UserService
 * ==============================================================================================
 * 
 * WHY TRANSACTIONAL BOUNDARIES & READ-ONLY OPTIMIZATIONS ARE APPLIED:
 * 1. Read-Only Transaction Default (@Transactional(readOnly = true)):
 *    - Reduces overhead on user lookups, authentication queries, and user lists.
 * 2. Explicit Mutation Transactions (@Transactional):
 *    - Ensures atomic user registration, password updates, and soft deletions.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public UserDTO createUser(CreateUserRequest request, String currentUsername) {
        User caller = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new UsernameNotFoundException(currentUsername));

        // Enforce role hierarchy: ADMIN cannot create ADMIN or SYSTEM_ADMIN
        if (caller.getRole() == UserRole.ADMIN &&
                (request.getRole() == UserRole.ADMIN || request.getRole() == UserRole.SYSTEM_ADMIN)) {
            throw new InvalidOperationException("Admins cannot create Admin or System Admin accounts.");
        }

        String username = request.getUsername().trim();
        String email = request.getEmail().trim();

        if (userRepository.findByUsername(username).isPresent()) {
            throw new DuplicateResourceException("User", "username", username);
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new DuplicateResourceException("User", "email", email);
        }

        String rawPassword = (request.getPassword() != null && !request.getPassword().trim().isEmpty())
                ? request.getPassword().trim()
                : username + "@123";

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(request.getRole());

        userRepository.save(user);
        log.info("User created: username={}, role={}, createdBy={}", user.getUsername(), user.getRole(), currentUsername);
        return userMapper.toDTO(user);
    }

    @Transactional
    public BulkUploadResultDTO bulkUploadUsersCsv(MultipartFile file, String currentUsername) {
        User caller = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new UsernameNotFoundException(currentUsername));

        if (file == null || file.isEmpty()) {
            throw new InvalidOperationException("Uploaded file is empty.");
        }

        int totalRows = 0;
        int successCount = 0;
        int failureCount = 0;
        List<String> errors = new ArrayList<>();
        Set<String> batchUsernames = new HashSet<>();
        Set<String> batchEmails = new HashSet<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }

                // Check header row
                if (lineNumber == 1 && trimmed.toLowerCase().startsWith("username")) {
                    continue;
                }

                totalRows++;
                String[] tokens = trimmed.split(",", -1);
                if (tokens.length < 3) {
                    failureCount++;
                    errors.add(String.format("Row %d: Invalid column count. Expected format: username,email,password,role", lineNumber));
                    continue;
                }

                String username = tokens[0].trim();
                String email = tokens[1].trim();
                String password = tokens.length > 2 ? tokens[2].trim() : "";
                String roleStr = tokens.length > 3 ? tokens[3].trim() : "CUSTOMER";

                if (username.length() < 3) {
                    failureCount++;
                    errors.add(String.format("Row %d: Username must be at least 3 characters.", lineNumber));
                    continue;
                }

                if (!email.contains("@")) {
                    failureCount++;
                    errors.add(String.format("Row %d (%s): Invalid email format '%s'.", lineNumber, username, email));
                    continue;
                }

                UserRole role;
                try {
                    role = UserRole.valueOf(roleStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    failureCount++;
                    errors.add(String.format("Row %d (%s): Invalid role '%s'.", lineNumber, username, roleStr));
                    continue;
                }

                // Enforce role hierarchy: ADMIN cannot create ADMIN or SYSTEM_ADMIN
                if (caller.getRole() == UserRole.ADMIN && (role == UserRole.ADMIN || role == UserRole.SYSTEM_ADMIN)) {
                    failureCount++;
                    errors.add(String.format("Row %d (%s): Admins cannot create %s accounts.", lineNumber, username, role));
                    continue;
                }

                // Check database & intra-batch uniqueness
                if (userRepository.findByUsername(username).isPresent() || batchUsernames.contains(username.toLowerCase())) {
                    failureCount++;
                    errors.add(String.format("Row %d: Username '%s' already exists.", lineNumber, username));
                    continue;
                }

                if (userRepository.findByEmail(email).isPresent() || batchEmails.contains(email.toLowerCase())) {
                    failureCount++;
                    errors.add(String.format("Row %d: Email '%s' already exists.", lineNumber, email));
                    continue;
                }

                String effectivePassword = password.isEmpty() ? username + "@123" : password;

                User newUser = new User();
                newUser.setUsername(username);
                newUser.setEmail(email);
                newUser.setPassword(passwordEncoder.encode(effectivePassword));
                newUser.setRole(role);

                userRepository.save(newUser);
                batchUsernames.add(username.toLowerCase());
                batchEmails.add(email.toLowerCase());
                successCount++;
            }
        } catch (Exception e) {
            throw new InvalidOperationException("Failed to parse CSV file: " + e.getMessage());
        }

        log.info("Bulk upload processed by '{}': totalRows={}, successCount={}, failureCount={}", currentUsername, totalRows, successCount, failureCount);

        return BulkUploadResultDTO.builder()
                .totalRows(totalRows)
                .successCount(successCount)
                .failureCount(failureCount)
                .errors(errors)
                .build();
    }

    @Transactional
    public UserDTO registerUser(UserDTO dto) {
        User user = userMapper.toEntity(dto);

        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new DuplicateResourceException("User", "username", user.getUsername());
        }
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new DuplicateResourceException("User", "email", user.getEmail());
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
        log.info("User self-registered: username={}, role={}", user.getUsername(), user.getRole());
        return userMapper.toDTO(user);
    }

    public UserDTO findByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));

        return userMapper.toDTO(user);
    }

    public UserDTO findById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        return userMapper.toDTO(user);
    }

    @Transactional
    public UserDTO updateUser(Long id, UserDTO dto) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        userMapper.updateEntityFromDTO(dto, existingUser);

        if (dto.getPassword() != null) {
            existingUser.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        userRepository.save(existingUser);
        log.info("Profile updated for userId={}", id);
        return userMapper.toDTO(existingUser);
    }

    @Transactional
    public void updatePassword(Long userId, PasswordChangeDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new InvalidOperationException("Current password does not match.");
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
        log.info("Password successfully updated for userId={}", userId);
    }

    @Transactional
    public void softDeleteUser(Long userId) {
        softDeleteUser(userId, null);
    }

    @Transactional
    public void softDeleteUser(Long userId, String currentUsername) {
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (currentUsername != null) {
            User caller = userRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new UsernameNotFoundException(currentUsername));

            if (caller.getId().equals(target.getId())) {
                throw new InvalidOperationException("You cannot deactivate your own account.");
            }

            if (caller.getRole() == UserRole.ADMIN && (target.getRole() == UserRole.ADMIN || target.getRole() == UserRole.SYSTEM_ADMIN)) {
                throw new InvalidOperationException("Admins cannot deactivate Admin or System Admin accounts.");
            }
        }

        target.setDeleted(true);
        userRepository.save(target);
        log.warn("User soft-deleted: userId={}, username={}, deletedBy={}", userId, target.getUsername(), currentUsername != null ? currentUsername : "SYSTEM");
    }


    public List<UserDTO> getUsersByRole(UserRole role) {
        List<User> users = userRepository.findByRole(role);
        return userMapper.toDTOList(users);
    }

    public Page<UserDTO> getUsersByRole(UserRole role, Pageable pageable) {
        return userRepository.findByRole(role, pageable).map(userMapper::toDTO);
    }

    public List<UserDTO> getAllUsers() {
        List<User> users = userRepository.findAll();
        return userMapper.toDTOList(users);
    }

    public Page<UserDTO> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toDTO);
    }

    public List<UserDTO> getAllUsersIncludingDeleted() {
        List<User> users = userRepository.findAllIncludingDeleted();
        return userMapper.toDTOList(users);
    }
}
