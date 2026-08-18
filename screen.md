#	Screen Name	Route / Path	Key Features & UI Elements
1	Landing / Welcome Page	/	Platform highlights, features overview, "Sign In" and "Create Account" CTAs, dark/light theme switch.
2	Login Screen	/auth/login	Email/Username and Password inputs, Role selector (demo/fast switch), "Remember me", validation errors, and link to Register.
3	Register / Sign Up Screen	/auth/register	Name, Email, Password, Role selection (Customer / Agent), form validation feedback.
4	Forgot / Reset Password (Optional/Phase 2)	/auth/forgot-password	Email submission, password reset instructions/OTP simulation.
2. Customer Portal Screens
For end-users raising and tracking support tickets.

#	Screen Name	Route / Path	Key Features & UI Elements
5	Customer Dashboard	/customer/dashboard	Metric summary cards (Total Open, In Progress, Resolved), recent tickets table, quick "Create Ticket" floating button.
6	Create New Ticket Screen	/customer/tickets/new	Title, description, category selector, priority hint, file attachment dropzone, and real-time AI category/priority suggestion preview.
7	My Tickets List Screen	/customer/tickets	Search bar, status filters (OPEN, IN_PROGRESS, RESOLVED, CLOSED), sorting by date/priority, paginated list.
8	Customer Ticket Detail & Chat Screen	/customer/tickets/:id	Full conversation thread (Customer, Agent, and Auto-Responder AI bot), status badge, status history timeline, message input box, option to mark as resolved / close ticket.
9	Customer Profile & Preferences	/customer/profile	Personal details, email notification toggles, password update.
3. Support Agent Portal Screens
For agents handling, replying to, and resolving tickets.

#	Screen Name	Route / Path	Key Features & UI Elements
10	Agent Dashboard	/agent/dashboard	Workload overview (Assigned to Me, Unassigned Queue, SLA Urgent Tickets), resolution stats, daily performance chart.
11	Agent Ticket Queue / Triage Screen	/agent/tickets	Tabbed queue (Assigned to Me, Unassigned, All), bulk assignment, filter by Sentiment (Angry/Neutral/Happy), Priority, and AI Tag.
12	Agent Ticket Workspace & AI Copilot	/agent/tickets/:id	Split-view interface:
• Left/Center: Ticket info, status switcher, customer details, conversation thread, private internal notes vs. public customer reply.
• Right Panel (AI Copilot): 3-bullet AI thread summary, customer sentiment badge, AI auto-suggested replies (with 1-click insert).
13	Knowledge Base / Canned Responses	/agent/knowledge-base	Searchable quick templates and FAQs for rapid ticket responses.
4. Admin Portal Screens
For system administrators managing team, configuration, and monitoring AI agents.

#	Screen Name	Route / Path	Key Features & UI Elements
14	Admin Executive Dashboard & Analytics	/admin/dashboard	High-level metrics: Average Resolution Time (MTTR), SLA breach rate, ticket volume trends, agent leaderboard, AI triage success rate.
15	User & Team Management Screen	/admin/users	List of all Users, Agents, and Admins; create/edit users, assign roles, activate/deactivate accounts.
16	Category & Routing Rules Screen	/admin/settings/routing	Manage ticket categories (e.g., Billing, Technical, Account), define SLA response thresholds and auto-assignment rules.
17	AI Agent Configuration & Logs	/admin/settings/ai	Gemini model status, prompt tuning for Triage/Auto-Responder, API usage stats, and AI triage audit log.
18	Audit Logs & System Activity	/admin/audit-logs	Comprehensive log of status transitions, ticket assignments, and agent activities.
5. Shared & Utility Components / Modals
Reused across all dashboards.

#	Screen / Modal Name	Purpose
19	Notification Center / Drawer	Slide-out panel for real-time ticket alerts, status updates, and mentions.
20	Ticket Quick-View Drawer / Modal	Slide-over drawer to inspect ticket details from list views without full page reload.
21	404 Not Found & Error Screens (403 / 500)	Graceful fallback screens for missing routes or unauthorized access.
Summary Checklist for Implementation:
Phase 1 (MVP): Screens 1, 2, 3, 5, 6, 7, 8, 10, 11, 12 (Core Auth, Customer Portal, and Agent Queue).
Phase 2 & 3 (AI & Admin): Screens 12 (AI Copilot panel), 14, 15, 16, 17 (Admin & AI Config).



