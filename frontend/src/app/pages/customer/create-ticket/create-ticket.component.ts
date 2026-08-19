import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MockDataService } from '../../../services/mock-data.service';
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
  private mockData = inject(MockDataService);
  private router = inject(Router);

  ticketForm: FormGroup = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(100)]],
    description: ['', [Validators.required]],
    priority: [TicketPriority.MEDIUM, [Validators.required]],
  });

  isLoading = false;
  showSuccess = false;
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
    const { title, description, priority } = this.ticketForm.value;

    // Simulate API delay
    setTimeout(() => {
      this.mockData.createTicket({ title, description, priority });
      this.isLoading = false;
      this.showSuccess = true;

      setTimeout(() => {
        this.router.navigate(['/customer/tickets']);
      }, 1200);
    }, 800);
  }
}
