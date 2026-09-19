import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { UserRole } from '../models/user.model';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    if (authService.mustChangePassword()) {
      router.navigate(['/auth/login'], { queryParams: { forcePasswordChange: 'true' } });
      return false;
    }
    return true;
  }

  router.navigate(['/auth/login']);
  return false;
};

export const roleGuard = (allowedRoles: UserRole[]): CanActivateFn => {
  return () => {
    const authService = inject(AuthService);
    const router = inject(Router);

    if (!authService.isAuthenticated()) {
      router.navigate(['/auth/login']);
      return false;
    }

    if (authService.mustChangePassword()) {
      router.navigate(['/auth/login'], { queryParams: { forcePasswordChange: 'true' } });
      return false;
    }

    const currentRole = authService.userRole();
    if (currentRole && allowedRoles.includes(currentRole)) {
      return true;
    }

    // Redirect to proper portal based on actual role
    if (currentRole === UserRole.CUSTOMER) {
      router.navigate(['/customer/dashboard']);
    } else if (currentRole === UserRole.SUPPORT_AGENT) {
      router.navigate(['/agent/dashboard']);
    } else if (currentRole === UserRole.ADMIN) {
      router.navigate(['/admin/dashboard']);
    } else {
      router.navigate(['/auth/login']);
    }

    return false;
  };
};
