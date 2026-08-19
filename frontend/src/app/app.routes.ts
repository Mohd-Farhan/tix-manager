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
    path: '**',
    redirectTo: '',
  }
];
