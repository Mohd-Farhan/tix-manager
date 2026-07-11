// TODO: UserService Class Definition
// Package: com.support.service
//
// Description:
// Business logic service for handling User operations like registration and search.
//
// Annotations: @Service
//
// Fields to Inject:
// - UserRepository userRepository
// - PasswordEncoder passwordEncoder (to encrypt passwords before storing)
//
// Methods to Implement:
//
// 1. public User registerUser(User user)
//    - Check if username or email already exists in UserRepository. If yes, throw runtime exception.
//    - Encrypt user.getPassword() using passwordEncoder.encode().
//    - Set the encrypted password back to the user object.
//    - Save the user and return it.
//
// 2. public User findByUsername(String username)
//    - Fetch user from UserRepository. If not present, throw EntityNotFoundException (or UsernameNotFoundException).
//    - Return the found user.
//
// Java Code Blueprint:
//
// package com.support.service;
//
// import com.support.entity.User;
// import com.support.repository.UserRepository;
// import org.springframework.security.crypto.password.PasswordEncoder;
// import org.springframework.stereotype.Service;
//
// @Service
// public class UserService {
//     // Inject dependencies and implement methods here.
// }
