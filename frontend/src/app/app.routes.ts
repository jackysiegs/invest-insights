import { Routes } from '@angular/router';
import { LoginComponent } from './login/login.component';
import { MainAppComponent } from './main-app.component';
import { PortfolioDetailComponent } from './portfolio-detail/portfolio-detail.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'dashboard', component: MainAppComponent },
  { path: 'portfolio/:id', component: PortfolioDetailComponent },
  { path: '', redirectTo: '/login', pathMatch: 'full' }
];
