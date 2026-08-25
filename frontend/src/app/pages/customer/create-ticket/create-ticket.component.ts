import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TicketService } from '../../../services/ticket.service';
import { AuthService } from '../../../services/auth.service';
import { TicketPriority } from '../../../models/ticket.model';

@Component({
  selector: 'app-create-ticket',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule],
  templateUrl: './create-ticket.component.html',
  styleUrl: './create-ticket.component.css',
})
export class CreateTicketComponent {
  private fb = inject(FormBuilder);
  private ticketService = inject(TicketService);
  private authService = inject(AuthService);
  private router = inject(Router);

  ticketForm: FormGroup = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(100)]],
    description: ['', [Validators.required]],
    priority: [TicketPriority.MEDIUM, [Validators.required]],
  });

  isLoading = false;
  showSuccess = false;
  errorMessage = '';
  priorities = [
    { value: TicketPriority.LOW, label: 'Low', description: 'Minor issue, no urgency' },
    { value: TicketPriority.MEDIUM, label: 'Medium', description: 'Moderate impact, standard resolution' },
    { value: TicketPriority.HIGH, label: 'High', description: 'Critical issue, needs immediate attention' },
  ];

  get f() { return this.ticketForm.controls; }

  get titleLength(): number {
    return this.f['title'].value?.length ?? 0;
  }

  onSubmit(): void {
    if (this.ticketForm.invalid) {
      this.ticketForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    const { title, description, priority } = this.ticketForm.value;
    const user = this.authService.getCurrentUser();

    this.ticketService.createTicket({
      title,
      description,
      priority,
      customerId: user?.id
    }).subscribe({
      next: () => {
        this.isLoading = false;
        this.showSuccess = true;
        setTimeout(() => {
          this.router.navigate(['/customer/tickets']);
        }, 1200);
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || err.error || 'Failed to create ticket.';
      }
    });
  }
}
