// TODO: Register Page
//
// Description:
// Clean form to register a new user in the platform database.
//
// Instructions:
// 1. Create a stateful form with fields: `username`, `email`, `password`, `confirmPassword`, and `role` selection.
// 2. Client-side Validation:
//    - Email matches email format.
//    - Username length >= 3.
//    - Password >= 6 characters.
//    - Password and confirmPassword match.
// 3. Implement submit handler:
//    - Post details to `/api/auth/register`.
//    - On success: Show registration success popup/alert, navigate to `/login`.
//    - On error: Render backend validation messages.
// 4. Role Selection dropdown/radio: Custom option selection to register as `CUSTOMER` or `SUPPORT_AGENT` (for development demonstration).
//
// React Code Blueprint:
//
// import React, { useState } from 'react';
// import { useNavigate, Link } from 'react-router-dom';
// import axios from 'axios';
//
// export default function Register() {
//     // Form handlers, API communication, navigation routing
// }
