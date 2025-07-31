import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-portfolio-create-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './portfolio-create-modal.component.html',
  styleUrl: './portfolio-create-modal.component.scss'
})
export class PortfolioCreateModalComponent {
  @Input() isOpen: boolean = false;
  @Input() selectedClient: any = null;
  @Output() closeModal = new EventEmitter<void>();
  @Output() portfolioCreated = new EventEmitter<any>();

  // Form data
  portfolioName: string = '';
  accountType: string = '';
  totalValue: number = 0;

  // Form validation
  isSubmitting: boolean = false;
  errors: string[] = [];
  fieldErrors: { [key: string]: string } = {};

  // Account type options
  accountTypes = [
    { value: 'Brokerage', label: 'Brokerage Account' },
    { value: 'Roth IRA', label: 'Roth IRA' },
    { value: 'Traditional IRA', label: 'Traditional IRA' },
    { value: '401k', label: '401(k)' },
    { value: '403b', label: '403(b)' },
    { value: 'SEP IRA', label: 'SEP IRA' },
    { value: 'Simple IRA', label: 'Simple IRA' },
    { value: '529 Plan', label: '529 College Savings Plan' },
    { value: 'HSA', label: 'Health Savings Account (HSA)' },
    { value: 'Other', label: 'Other' }
  ];

  constructor(private http: HttpClient) {}

  onClose(): void {
    this.resetForm();
    this.closeModal.emit();
  }

  onBackdropClick(event: Event): void {
    if (event.target === event.currentTarget) {
      this.onClose();
    }
  }

  validateForm(): boolean {
    this.errors = [];
    this.fieldErrors = {};

    if (!this.portfolioName.trim()) {
      this.fieldErrors['portfolioName'] = 'Portfolio name is required';
    }

    if (!this.accountType) {
      this.fieldErrors['accountType'] = 'Account type is required';
    }

    if (!this.totalValue || this.totalValue <= 0) {
      this.fieldErrors['totalValue'] = 'Total value must be greater than 0';
    }

    if (!this.selectedClient) {
      this.errors.push('A client must be selected');
    }

    return this.errors.length === 0 && Object.keys(this.fieldErrors).length === 0;
  }

  onSubmit(): void {
    if (!this.validateForm()) {
      return;
    }

    this.isSubmitting = true;

    const portfolioData = {
      name: this.portfolioName.trim(),
      accountType: this.accountType,
      totalValue: this.totalValue,
      client: {
        id: this.selectedClient.id
      }
    };

    this.http.post<any>('http://localhost:8080/api/portfolios', portfolioData)
      .subscribe({
        next: (response) => {
          console.log('Portfolio created successfully:', response);
          this.portfolioCreated.emit(response);
          this.resetForm();
          this.onClose();
        },
        error: (error) => {
          console.error('Error creating portfolio:', error);
          this.errors.push('Failed to create portfolio. Please try again.');
          this.isSubmitting = false;
        }
      });
  }

  resetForm(): void {
    this.portfolioName = '';
    this.accountType = '';
    this.totalValue = 0;
    this.errors = [];
    this.fieldErrors = {};
    this.isSubmitting = false;
  }

  // Prevent modal from closing when clicking inside the modal content
  onModalContentClick(event: Event): void {
    event.stopPropagation();
  }

  // Clear placeholder text on focus
  onPortfolioNameFocus(event: Event): void {
    const input = event.target as HTMLInputElement;
    input.placeholder = '';
  }

  onTotalValueFocus(event: Event): void {
    const input = event.target as HTMLInputElement;
    input.placeholder = '';
  }
} 