import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors } from '@angular/forms';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule],
  templateUrl: './register.component.html',
  styleUrl: './register.component.css',
})
export class RegisterComponent implements OnInit {
  private fb = inject(FormBuilder);
  private router = inject(Router);

  registerForm!: FormGroup;
  showPassword = false;
  showConfirmPassword = false;
  isLoading = false;
  errorMessage = '';

  isDark = false;

  roles = [
    { value: 'CUSTOMER', label: 'Customer' },
    { value: 'SUPPORT_AGENT', label: 'Support Agent' },
  ];

  ngOnInit(): void {
    this.registerForm = this.fb.group(
      {
        username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50)]],
        email: ['', [Validators.required, Validators.email]],
        password: ['', [Validators.required, Validators.minLength(6)]],
        confirmPassword: ['', [Validators.required]],
        role: ['CUSTOMER', [Validators.required]],
      },
      { validators: this.passwordMatchValidator }
    );

    const savedTheme = localStorage.getItem('tix-theme');
    this.isDark = savedTheme === 'dark';
    document.documentElement.setAttribute('data-theme', this.isDark ? 'dark' : 'light');
  }

  passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const password = control.get('password');
    const confirmPassword = control.get('confirmPassword');
    if (password && confirmPassword && password.value !== confirmPassword.value) {
      confirmPassword.setErrors({ passwordMismatch: true });
      return { passwordMismatch: true };
    }
    return null;
  }

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  toggleConfirmPassword(): void {
    this.showConfirmPassword = !this.showConfirmPassword;
  }

  toggleTheme(): void {
    this.isDark = !this.isDark;
    document.documentElement.setAttribute('data-theme', this.isDark ? 'dark' : 'light');
    localStorage.setItem('tix-theme', this.isDark ? 'dark' : 'light');
  }

  onSubmit(): void {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    // Build the payload matching UserDTO: { username, email, password, role }
    const { username, email, password, role } = this.registerForm.value;
    const payload = { username, email, password, role };
    console.log('Register payload:', payload);

    // TODO: Connect to AuthService -> POST /api/auth/register
    setTimeout(() => {
      this.isLoading = false;
      // On success: this.router.navigate(['/auth/login']);
    }, 1500);
  }

  get f() {
    return this.registerForm.controls;
  }
}
