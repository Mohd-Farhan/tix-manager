// TODO: Login Page
//
// Description:
// Clean glassmorphic portal where users can authenticate.
//
// Instructions:
// 1. Create a stateful form with fields: `username` and `password`.
// 2. Validate inputs: ensure fields are not empty, show validation errors.
// 3. Implement submit handler:
//    - Perform POST request to `/api/auth/login` with username & password.
//    - On Success: Save JWT token/user profile data to localStorage (or React Context).
//    - Redirect to `/dashboard`.
//    - On Error: Show alert or form-level error message.
// 4. Link to registration page: "Don't have an account? Register".
// 5. Use visual styling: Centered card layout, gradient background, custom inputs, transitions.
//
// React Code Blueprint:
//
// import React, { useState } from 'react';
// import { useNavigate, Link } from 'react-router-dom';
// import axios from 'axios';
//
// export default function Login() {
//     // Form handlers, API communication, navigation routing
// }
