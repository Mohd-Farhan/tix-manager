package com.support.service;

import com.support.dto.PasswordChangeDTO;
import com.support.dto.UserDTO;
import com.support.entity.User;
import com.support.entity.UserRole;
import com.support.exception.DuplicateResourceException;
import com.support.exception.InvalidOperationException;
import com.support.exception.ResourceNotFoundException;
import com.support.mapper.UserMapper;
import com.support.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

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

    public UserDTO updateUser(Long id, UserDTO dto) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        userMapper.updateEntityFromDTO(dto, existingUser);

        if (dto.getPassword() != null) {
            existingUser.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        userRepository.save(existingUser);
        return userMapper.toDTO(existingUser);
    }

    public void updatePassword(Long userId, PasswordChangeDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new InvalidOperationException("Current password does not match.");
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
    }

    public void softDeleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        user.setDeleted(true);
        userRepository.save(user);
    }

    public List<UserDTO> getUsersByRole(UserRole role) {
        List<User> users = userRepository.findByRole(role);
        return userMapper.toDTOList(users);
    }

    public List<UserDTO> getAllUsers() {
        List<User> users = userRepository.findAll();
        return userMapper.toDTOList(users);
    }

    public List<UserDTO> getAllUsersIncludingDeleted() {
        List<User> users = userRepository.findAllIncludingDeleted();
        return userMapper.toDTOList(users);
    }
}
