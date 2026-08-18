import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css',
})
export class LoginComponent implements OnInit {
  private fb = inject(FormBuilder);
  private router = inject(Router);

  loginForm!: FormGroup;
  showPassword = false;
  isLoading = false;
  errorMessage = '';

  isDark = false;

  ngOnInit(): void {
    this.loginForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      password: ['', [Validators.required, Validators.minLength(6)]],
    });

    const savedTheme = localStorage.getItem('tix-theme');
    this.isDark = savedTheme === 'dark';
    document.documentElement.setAttribute('data-theme', this.isDark ? 'dark' : 'light');
  }

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  toggleTheme(): void {
    this.isDark = !this.isDark;
    document.documentElement.setAttribute('data-theme', this.isDark ? 'dark' : 'light');
    localStorage.setItem('tix-theme', this.isDark ? 'dark' : 'light');
  }

  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    // TODO: Connect to AuthService -> POST /api/auth/login
    const { username, password } = this.loginForm.value;
    console.log('Login payload:', { username, password });

    // Simulate API call
    setTimeout(() => {
      this.isLoading = false;
      // On success: this.router.navigate(['/dashboard']);
    }, 1500);
  }

  get f() {
    return this.loginForm.controls;
  }
}
