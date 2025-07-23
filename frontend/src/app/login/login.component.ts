import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { Router } from '@angular/router';

interface LoginRequest {
  username: string;
  password: string;
}

interface LoginResponse {
  advisorId: number;
  advisorName: string;
  email: string;
  token: string;
  clients: Client[];
}

interface Client {
  id: number;
  name: string;
  email: string;
  riskTolerance: string;
  investmentGoals: string;
  timeHorizon: number;
  annualIncomeGoal: number;
}

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, HttpClientModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  username: string = '';
  password: string = '';
  isLoading: boolean = false;
  errorMessage: string = '';
  showPassword: boolean = false;

  constructor(
    private http: HttpClient,
    private router: Router
  ) {}

  async onLogin() {
    if (!this.username || !this.password) {
      this.errorMessage = 'Please enter both username and password';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    try {
      const loginData: LoginRequest = {
        username: this.username,
        password: this.password
      };

      console.log('Making login request to:', '/api/auth/login');
      console.log('Login data:', loginData);

      const response = await firstValueFrom(
        this.http.post<LoginResponse>('http://localhost:8080/api/auth/login', loginData)
      );

      console.log('Login response:', response);

      // Store authentication data
      localStorage.setItem('advisorToken', response.token);
      localStorage.setItem('advisorId', response.advisorId.toString());
      localStorage.setItem('advisorName', response.advisorName);
      localStorage.setItem('advisorEmail', response.email);
      localStorage.setItem('clients', JSON.stringify(response.clients));

      // Navigate to dashboard
      this.router.navigate(['/dashboard']);
      
    } catch (error: any) {
      console.error('Login error:', error);
      console.error('Error status:', error.status);
      console.error('Error message:', error.message);
      if (error.status === 401) {
        this.errorMessage = 'Invalid username or password';
      } else if (error.status === 0) {
        this.errorMessage = 'Unable to connect to server. Please check your connection.';
      } else if (error.status === 403) {
        this.errorMessage = 'Access forbidden. Please check server configuration.';
      } else {
        this.errorMessage = 'Login failed. Please try again.';
      }
    } finally {
      this.isLoading = false;
    }
  }

  togglePasswordVisibility() {
    this.showPassword = !this.showPassword;
  }

  onKeyPress(event: KeyboardEvent) {
    if (event.key === 'Enter') {
      this.onLogin();
    }
  }
} 