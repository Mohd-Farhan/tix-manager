// TODO: Navbar Component
//
// Description:
// Top navigation bar showing logo, user session details, and navigation controls.
//
// Instructions:
// 1. Fetch authenticated user data (username, role) from localStorage/Context.
// 2. Render navigation links depending on UserRole:
//    - If Customer: show "My Tickets", "Create Ticket".
//    - If Support Agent: show "Ticket Queue", "My Tasks".
//    - If Admin: show "All Tickets", "User Management".
// 3. Render Username and Role badge.
// 4. Implement "Logout" button: clears local storage token/user state, redirects to "/login".
// 5. Use CSS classes from index.css for styling (e.g. flex row alignment, glass background, active link states).
//
// React Code Blueprint:
//
// import { useNavigate, Link } from 'react-router-dom';
//
// export default function Navbar() {
//     // Implementation logic here
// }
