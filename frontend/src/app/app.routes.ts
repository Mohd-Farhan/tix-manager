import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/landing/landing.component').then((m) => m.LandingComponent),
  },
  {
    path: 'auth/login',
    loadComponent: () =>
      import('./pages/auth/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'auth/register',
    loadComponent: () =>
      import('./pages/auth/register/register.component').then(
        (m) => m.RegisterComponent
      ),
  },
  {
    path: 'customer',
    loadComponent: () => import('./layouts/customer-layout/customer-layout.component').then((m) => m.CustomerLayoutComponent),
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', loadComponent: () => import('./pages/customer/dashboard/dashboard.component').then((m) => m.DashboardComponent) },
      { path: 'tickets', loadComponent: () => import('./pages/customer/ticket-list/ticket-list.component').then((m) => m.TicketListComponent) },
      { path: 'tickets/:id', loadComponent: () => import('./pages/customer/ticket-detail/ticket-detail.component').then((m) => m.TicketDetailComponent) },
    ]
  },
  {
    path: 'agent',
    loadComponent: () => import('./layouts/agent-layout/agent-layout.component').then((m) => m.AgentLayoutComponent),
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', loadComponent: () => import('./pages/agent/dashboard/dashboard.component').then((m) => m.DashboardComponent) },
      { path: 'tickets', loadComponent: () => import('./pages/agent/ticket-queue/ticket-queue.component').then((m) => m.TicketQueueComponent) },
      { path: 'tickets/:id', loadComponent: () => import('./pages/agent/ticket-detail/ticket-detail.component').then((m) => m.TicketDetailComponent) },
    ]
  },
  {
    path: 'admin',
    loadComponent: () => import('./layouts/admin-layout/admin-layout.component').then((m) => m.AdminLayoutComponent),
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', loadComponent: () => import('./pages/admin/dashboard/dashboard.component').then((m) => m.AdminDashboardComponent) },
      { path: 'users', loadComponent: () => import('./pages/admin/user-management/user-management.component').then((m) => m.UserManagementComponent) },
      { path: 'tickets', loadComponent: () => import('./pages/admin/ticket-oversight/ticket-oversight.component').then((m) => m.TicketOversightComponent) },
      { path: 'settings', loadComponent: () => import('./pages/admin/system-settings/system-settings.component').then((m) => m.SystemSettingsComponent) },
    ]
  },
  {
    path: '**',
    redirectTo: '',
  }
];
