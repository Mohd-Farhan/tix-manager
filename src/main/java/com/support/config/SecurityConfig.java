// TODO: Spring Security Configuration Class
// Package: com.support.config
//
// Description:
// Configures application security, including path access permissions, password hashing, and CORS filters.
//
// Annotations:
// - @Configuration, @EnableWebSecurity
//
// Configuration Requirements:
// 1. Password Encoder Bean:
//    - Define BCryptPasswordEncoder as a @Bean for hashing passwords.
//
// 2. Security Filter Chain Bean:
//    - Define a SecurityFilterChain bean.
//    - Disable CSRF (since we will build stateless REST APIs, or configure CSRF tokens if stateful).
//    - Enable CORS (allowing requests from React frontend, default localhost:5173).
//    - Configure Endpoint Authorizations:
//      - Allow public access to register & login endpoints: "/api/auth/**" and H2 Console: "/h2-console/**" (frame options must be disabled for H2 console to render in frames).
//      - Require CUSTOMER role for creating tickets: POST "/api/tickets"
//      - Require SUPPORT_AGENT or ADMIN role for assigning or closing tickets: PUT "/api/tickets/*/assign", PUT "/api/tickets/*/status"
//      - Allow authenticated users access to read tickets and messages: GET "/api/tickets/**"
//      - Any other request must be fully authenticated.
//    - If using stateless authentication: Configure SessionCreationPolicy.STATELESS and add a custom JWT authentication filter before UsernamePasswordAuthenticationFilter.
//    - If using basic auth for simplicity: Configure httpBasic().
//
// Java Code Blueprint:
//
// package com.support.config;
//
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.security.config.annotation.web.builders.HttpSecurity;
// import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
// import org.springframework.security.web.SecurityFilterChain;
//
// @Configuration
// @EnableWebSecurity
// public class SecurityConfig {
//     // Define BCryptPasswordEncoder bean
//     // Define SecurityFilterChain bean with filter and routing configuration
// }
