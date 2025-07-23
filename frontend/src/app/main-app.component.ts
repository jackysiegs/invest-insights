import { Component, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { timeout, catchError } from 'rxjs/operators';
import { of } from 'rxjs';
import { InsightsService, InvestmentInsight } from './services/insights.service';
import { InsightSectionComponent } from './components/insight-section/insight-section.component';

@Component({
  selector: 'app-main-app',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './main-app.component.html',
  styleUrl: './main-app.component.scss'
})
export class MainAppComponent implements OnDestroy {
  // Tab management
  activeTab: string = 'dashboard';
  
  // User info
  advisorName: string = 'Advisor Name';
  advisorEmail: string = 'advisor@example.com';
  
  // Client management
  clients: any[] = [];
  selectedClientId: string = '';
  selectedClient: any = null;
  clientSearchTerm: string = '';
  filteredClients: any[] = [];
  isClientDropdownOpen: boolean = false;
  
  // Recent users management
  recentUsers: any[] = [];
  recentUsersSearchTerm: string = '';
  filteredRecentUsers: any[] = [];
  isRecentUsersDropdownOpen: boolean = false;
  
  // Portfolio data
  portfolios: any[] = [];
  selectedPortfolioId: string = '';
  selectedPortfolio: any = null;
  portfoliosCount: number = 0;
  totalValue: number = 0; // Total invested value
  totalPortfolioValue: number = 0; // Total portfolio value (including cash)
  cashAmount: number = 0; // Cash amount
  holdingsCount: number = 0;
  holdings: any[] = []; // Holdings data for current portfolio
  sectorAnalysis: any = {}; // Sector breakdown data
  
  // Insights
  recentInsights: InvestmentInsight[] = [];
  currentInsight: InvestmentInsight | null = null;
  analysisPreferences: string = '';
  isGeneratingInsight: boolean = false;
  
  // Market data
  marketNews: any[] = [];
  lastNewsUpdate: Date | null = null;
  portfolioNewsCache: Map<string, { news: any[], timestamp: Date }> = new Map();
  
  // UI state
  isLoading: boolean = false;
  activeSectionTab: string = '';
  showDebugInfo: boolean = false;

  // Portfolio carousel properties
  currentPortfolioIndex: number = 0;
  portfolioCarouselInterval: any = null;

  constructor(
    private router: Router, 
    private http: HttpClient,
    private insightsService: InsightsService
  ) {
    // Load user data from localStorage on component initialization
    this.loadUserData();
    // Don't load all portfolios initially - only load when client is selected
    
    // Check for query parameters to set active tab and restore client selection
    this.router.events.subscribe((event: any) => {
      if (event.url && event.url.includes('tab=portfolios')) {
        this.activeTab = 'portfolios';
        
        // Restore client selection if coming back from portfolio detail
        const urlParams = new URLSearchParams(window.location.search);
        const clientId = urlParams.get('clientId');
        const clientName = urlParams.get('clientName');
        
        if (clientId && clientName) {
          // Find the client in the loaded clients array
          const client = this.clients.find(c => c.id.toString() === clientId);
          if (client) {
            this.selectedClientId = clientId;
            this.selectedClient = client;
            this.clientSearchTerm = clientName;
            this.loadPortfoliosForClient();
          }
        }
      }
    });
  }
  
  // Methods
  setActiveTab(tab: string): void {
    this.activeTab = tab;
    
    // Stop carousel when leaving dashboard
    if (tab !== 'dashboard') {
      this.stopPortfolioCarousel();
    } else if (this.portfolios.length > 0) {
      // Start carousel when returning to dashboard with portfolios
      this.startPortfolioCarousel();
    }
  }
  
  onClientChange(): void {
    this.selectedClient = this.clients.find(c => c.id.toString() === this.selectedClientId);
    
    // Store the currently selected portfolio ID before loading new portfolios
    const previouslySelectedPortfolioId = this.selectedPortfolioId;
    
    this.loadPortfoliosForClient(previouslySelectedPortfolioId);
    this.loadInsightsForClient();
    
    // Load general market news initially (will be updated when portfolio is selected)
    this.loadMarketNews();
  }

  onClientSearch(): void {
    if (!this.clientSearchTerm.trim()) {
      this.filteredClients = this.clients;
    } else {
      const searchTerm = this.clientSearchTerm.toLowerCase();
      this.filteredClients = this.clients.filter(client => 
        client.name.toLowerCase().includes(searchTerm) ||
        client.email.toLowerCase().includes(searchTerm) ||
        client.riskTolerance.toLowerCase().includes(searchTerm) ||
        client.investmentGoals.toLowerCase().includes(searchTerm)
      );
    }
  }

  onClientSearchFocus(): void {
    this.isClientDropdownOpen = true;
    this.filteredClients = this.clients;
  }

  onClientSearchBlur(): void {
    // Delay closing to allow for clicks on dropdown items
    setTimeout(() => {
      this.isClientDropdownOpen = false;
    }, 200);
  }

  selectClient(client: any): void {
    this.selectedClientId = client.id.toString();
    this.selectedClient = client;
    this.clientSearchTerm = client.name; // Show selected client name in search
    this.isClientDropdownOpen = false;
    
    // Add client to recent users
    this.addToRecentUsers(client);
    
    // Store selected client info in localStorage for navigation back
    localStorage.setItem('selectedClientId', client.id.toString());
    localStorage.setItem('selectedClientName', client.name);
    
    this.onClientChange();
  }

  // Recent users methods
  onRecentUsersSearch(): void {
    if (!this.recentUsersSearchTerm.trim()) {
      this.filteredRecentUsers = this.recentUsers;
    } else {
      const searchTerm = this.recentUsersSearchTerm.toLowerCase();
      this.filteredRecentUsers = this.recentUsers.filter(client => 
        client.name.toLowerCase().includes(searchTerm) ||
        client.email.toLowerCase().includes(searchTerm)
      );
    }
  }

  onRecentUsersSearchFocus(): void {
    this.isRecentUsersDropdownOpen = true;
    this.filteredRecentUsers = this.recentUsers;
    
    // If a client is selected, clear the search term to show all recent users
    if (this.selectedClient) {
      this.recentUsersSearchTerm = '';
    }
  }

  onRecentUsersSearchBlur(): void {
    // Delay closing to allow for clicks on dropdown items
    setTimeout(() => {
      this.isRecentUsersDropdownOpen = false;
    }, 200);
  }

  selectRecentUser(client: any): void {
    this.selectedClientId = client.id.toString();
    this.selectedClient = client;
    this.recentUsersSearchTerm = ''; // Clear search term
    this.isRecentUsersDropdownOpen = false;
    
    // Move selected user to top of recent users
    this.addToRecentUsers(client);
    
    // Store selected client info in localStorage for navigation back
    localStorage.setItem('selectedClientId', client.id.toString());
    localStorage.setItem('selectedClientName', client.name);
    
    this.onClientChange();
  }

  clearSelectedClient(): void {
    this.selectedClientId = '';
    this.selectedClient = null;
    this.recentUsersSearchTerm = '';
    this.selectedPortfolioId = '';
    this.selectedPortfolio = null;
    this.portfolios = [];
    this.recentInsights = [];
    this.currentInsight = null;
    
    // Clear from localStorage
    localStorage.removeItem('selectedClientId');
    localStorage.removeItem('selectedClientName');
    
    // Update portfolio overview
    this.updatePortfolioOverview();
  }

  addToRecentUsers(client: any): void {
    // Remove client if already exists
    this.recentUsers = this.recentUsers.filter(u => u.id !== client.id);
    
    // Add to beginning of array
    this.recentUsers.unshift(client);
    
    // Keep only top 3 most recent users
    this.recentUsers = this.recentUsers.slice(0, 3);
    
    // Update filtered list
    this.filteredRecentUsers = this.recentUsers;
    
    // Save to localStorage
    localStorage.setItem('recentUsers', JSON.stringify(this.recentUsers));
  }

  onPortfolioChange(): void {
    this.selectedPortfolio = this.portfolios.find(p => p.id.toString() === this.selectedPortfolioId);
    this.updatePortfolioOverview();
  }

  private loadPortfoliosForClient(previouslySelectedPortfolioId?: string): void {
    if (!this.selectedClientId) {
      this.portfolios = [];
      this.selectedPortfolioId = '';
      this.selectedPortfolio = null;
      this.updatePortfolioOverview();
      return;
    }

    console.log('Loading portfolios for client:', this.selectedClientId);
    this.isLoading = true;
    
    this.http.get<any[]>(`http://localhost:8080/api/portfolios/client/${this.selectedClientId}`)
      .subscribe({
        next: (portfolios) => {
          this.portfolios = portfolios;
          this.portfoliosCount = portfolios.length;
          console.log('Loaded portfolios for client:', portfolios);
          
          // Try to restore the previously selected portfolio if it exists for this client
          if (previouslySelectedPortfolioId) {
            const portfolioExists = portfolios.find(p => p.id.toString() === previouslySelectedPortfolioId);
            if (portfolioExists) {
              this.selectedPortfolioId = previouslySelectedPortfolioId;
              this.selectedPortfolio = portfolioExists;
            } else {
              // Clear selection if portfolio doesn't exist for this client
              this.selectedPortfolioId = '';
              this.selectedPortfolio = null;
            }
          } else {
            // Clear selection if no previously selected portfolio
            this.selectedPortfolioId = '';
            this.selectedPortfolio = null;
          }
          
          this.updatePortfolioOverview();
          
          // Start carousel if we have portfolios and we're on the dashboard
          if (this.portfolios.length > 0 && this.activeTab === 'dashboard') {
            this.currentPortfolioIndex = 0;
            this.updateCarouselPortfolio();
            this.startPortfolioCarousel();
          }
          
          this.isLoading = false;
        },
        error: (error) => {
          console.error('Error loading portfolios for client:', error);
          this.portfolios = [];
          this.portfoliosCount = 0;
          this.selectedPortfolioId = '';
          this.selectedPortfolio = null;
          this.updatePortfolioOverview();
          this.stopPortfolioCarousel();
          this.isLoading = false;
        }
      });
  }

  private loadPortfolios(): void {
    const advisorId = localStorage.getItem('advisorId');
    if (!advisorId) return;

    console.log('Loading all portfolios for advisor:', advisorId);
    this.isLoading = true;
    this.http.get<any[]>(`http://localhost:8080/api/portfolios/advisor/${advisorId}`)
      .subscribe({
        next: (portfolios) => {
          this.portfolios = portfolios;
          this.portfoliosCount = portfolios.length;
          console.log('Loaded all portfolios for advisor:', portfolios);
          this.isLoading = false;
        },
        error: (error) => {
          console.error('Error loading portfolios for advisor:', error);
          this.portfolios = [];
          this.portfoliosCount = 0;
          this.isLoading = false;
        }
      });
  }

  private updatePortfolioOverview(): void {
    if (!this.selectedPortfolio) {
      // Reset to default values or show aggregate data
      this.totalValue = 0;
      this.totalPortfolioValue = 0;
      this.cashAmount = 0;
      this.holdingsCount = 0;
      this.holdings = [];
      this.sectorAnalysis = {};
      return;
    }

    // Store total portfolio value (including cash)
    this.totalPortfolioValue = this.selectedPortfolio.totalValue || 0;

    // Load holdings to calculate actual invested value (same as backend logic)
    this.http.get<any[]>(`http://localhost:8080/api/portfolios/${this.selectedPortfolio.id}/holdings`)
      .subscribe({
        next: (holdings) => {
          this.holdings = holdings;
          this.holdingsCount = holdings.length;
          // Calculate total invested value from holdings (same as backend logic)
          this.totalValue = holdings.reduce((total, holding) => {
            return total + (holding.shares * holding.pricePerShare);
          }, 0);
          // Calculate cash amount
          this.cashAmount = this.totalPortfolioValue - this.totalValue;
          
          // Calculate sector analysis
          this.calculateSectorAnalysis(holdings);
        },
        error: (error) => {
          console.error('Error loading holdings count:', error);
          this.holdings = [];
          this.holdingsCount = 0;
          this.totalValue = 0;
          this.cashAmount = 0;
          this.sectorAnalysis = {};
        }
      });
  }

  private loadInsightsForClient(): void {
    if (!this.selectedClientId) {
      this.recentInsights = [];
      this.currentInsight = null;
      return;
    }

    this.insightsService.getInsightsByClient(this.selectedClientId).subscribe({
      next: (insights) => {
        // Debug: Log insights data received
        console.log('=== LOADED INSIGHTS DEBUG ===');
        console.log('Raw insights data:', JSON.stringify(insights, null, 2));
        insights.forEach((insight, index) => {
          console.log(`Insight ${index + 1}:`);
          console.log('  ID:', insight.id);
          console.log('  Portfolio Name:', insight.portfolioName);
          console.log('  Summary:', insight.summary);
          console.log('  Created At:', insight.createdAt);
        });
        console.log('=============================');
        
        // Sort insights in descending order (newest first) - using string comparison to avoid timezone issues
        this.recentInsights = insights.sort((a, b) => {
          // Use string comparison instead of Date objects to avoid timezone conversion
          return b.createdAt.localeCompare(a.createdAt);
        });
        
        // Parse structured data for each insight and store original dates
        this.recentInsights.forEach(insight => {
          // Always try to parse structured data, even if it already exists
          const parsed = this.insightsService.parseStructuredData(insight);
          if (parsed) {
            insight.structuredInsight = parsed;
          }
          
          // Store original date string to prevent timezone conversion issues
          if (typeof insight.createdAt === 'string') {
            (insight as any).originalCreatedAt = insight.createdAt;
          }
        });
        console.log('Loaded insights for client:', insights);
      },
      error: (error) => {
        console.error('Error loading insights for client:', error);
        this.recentInsights = [];
      }
    });
  }
  
  logout(): void {
    // Clear all authentication data from localStorage
    localStorage.removeItem('advisorToken');
    localStorage.removeItem('advisorId');
    localStorage.removeItem('advisorName');
    localStorage.removeItem('advisorEmail');
    localStorage.removeItem('clients');
    
    // Redirect to login page
    this.router.navigate(['/login']);
  }

  private loadUserData(): void {
    // Load user data from localStorage
    const storedName = localStorage.getItem('advisorName');
    const storedEmail = localStorage.getItem('advisorEmail');
    const storedClients = localStorage.getItem('clients');
    const storedRecentUsers = localStorage.getItem('recentUsers');
    
    if (storedName) {
      this.advisorName = storedName;
    }
    if (storedEmail) {
      this.advisorEmail = storedEmail;
    }
    if (storedClients) {
      try {
        this.clients = JSON.parse(storedClients);
        this.filteredClients = this.clients; // Initialize filtered clients
        console.log('Loaded clients from localStorage:', this.clients);
      } catch (e) {
        console.error('Error parsing clients data:', e);
        this.clients = [];
        this.filteredClients = [];
      }
    } else {
      // If no clients in localStorage, try to load them from the API
      this.loadClientsFromAPI();
    }
    
    // Load recent users from localStorage
    if (storedRecentUsers) {
      try {
        this.recentUsers = JSON.parse(storedRecentUsers);
        this.filteredRecentUsers = this.recentUsers; // Initialize filtered recent users
        console.log('Loaded recent users from localStorage:', this.recentUsers);
      } catch (e) {
        console.error('Error parsing recent users data:', e);
        this.recentUsers = [];
        this.filteredRecentUsers = [];
      }
    }
  }

  private loadClientsFromAPI(): void {
    const advisorId = localStorage.getItem('advisorId');
    if (!advisorId) {
      console.error('No advisor ID found in localStorage');
      return;
    }

    console.log('Loading clients from API for advisor:', advisorId);
    this.http.get<any[]>(`http://localhost:8080/api/auth/advisor/${advisorId}/clients`)
      .subscribe({
        next: (clients) => {
          this.clients = clients;
          this.filteredClients = clients; // Initialize filtered clients
          console.log('Loaded clients from API:', clients);
          // Store in localStorage for future use
          localStorage.setItem('clients', JSON.stringify(clients));
        },
        error: (error) => {
          console.error('Error loading clients from API:', error);
          this.clients = [];
          this.filteredClients = [];
        }
      });
  }
  
  generateInsight(): void {
    if (!this.selectedPortfolioId || this.isGeneratingInsight || !this.selectedClientId) {
      return;
    }

    this.isGeneratingInsight = true;
    
    this.insightsService.generateInsight(
      this.selectedPortfolioId, 
      this.analysisPreferences || 'Provide a comprehensive portfolio analysis',
      this.selectedClientId
    ).subscribe({
      next: (insight) => {
        // Debug: Log the insight data received
        console.log('=== INSIGHT RECEIVED DEBUG ===');
        console.log('Insight ID:', insight.id);
        console.log('Portfolio Name:', insight.portfolioName);
        console.log('Summary:', insight.summary);
        console.log('Created At:', insight.createdAt);
        console.log('================================');
        
        // Parse structured data if not already present
        if (!insight.structuredInsight) {
          const parsed = this.insightsService.parseStructuredData(insight);
          if (parsed) {
            insight.structuredInsight = parsed;
          }
        }
        
        // Store original date string to prevent timezone conversion issues
        if (typeof insight.createdAt === 'string') {
          (insight as any).originalCreatedAt = insight.createdAt;
        }
        
        this.currentInsight = insight;
        // Add new insight to the beginning of the list (it will be sorted properly on next load)
        this.recentInsights.unshift(insight);
        this.isGeneratingInsight = false;
      },
      error: (error) => {
        console.error('Error generating insight:', error);
        this.isGeneratingInsight = false;
      }
    });
  }
  
  addHolding(): void {
    // Add new holding
  }
  
  createPortfolio(): void {
    // Create new portfolio
  }
  
  viewPortfolio(portfolioId: string): void {
    // Navigate to portfolio detail page with client information
    this.router.navigate(['/portfolio', portfolioId], {
      queryParams: {
        clientId: this.selectedClientId,
        clientName: this.selectedClient?.name || ''
      }
    });
  }
  
  generatePortfolioInsight(portfolioId: string): void {
    // Generate insight for specific portfolio
  }
  
  viewInsight(insight: InvestmentInsight): void {
    // Debug: Log insight data
    console.log('=== VIEW INSIGHT DEBUG ===');
    console.log('Insight ID:', insight.id);
    console.log('Portfolio Name:', insight.portfolioName);
    console.log('Selected Portfolio:', this.selectedPortfolio);
    console.log('Selected Portfolio Name:', this.selectedPortfolio?.name);
    console.log('================================');
    
    // Parse structured data if not already present
    if (!insight.structuredInsight) {
      const parsed = this.insightsService.parseStructuredData(insight);
      if (parsed) {
        insight.structuredInsight = parsed;
      }
    }
    
    // Use original date string to prevent timezone conversion issues
    if ((insight as any).originalCreatedAt) {
      insight.createdAt = (insight as any).originalCreatedAt;
    }
    
    this.currentInsight = insight;
    
    // Reset active section tab to show the first section
    this.activeSectionTab = '';
  }

  viewInsightFromDashboard(insight: InvestmentInsight): void {
    // Switch to AI Insights tab
    this.setActiveTab('insights');
    
    // Set the current insight after a brief delay to ensure tab switch completes
    setTimeout(() => {
      // Parse structured data if not already present
      if (!insight.structuredInsight) {
        const parsed = this.insightsService.parseStructuredData(insight);
        if (parsed) {
          insight.structuredInsight = parsed;
        }
      }
      
      // Use original date string to prevent timezone conversion issues
      if ((insight as any).originalCreatedAt) {
        insight.createdAt = (insight as any).originalCreatedAt;
      }
      
      this.currentInsight = insight;
      this.activeSectionTab = '';
      this.showDebugInfo = false;
    }, 100);
  }
  
  formatInsightText(text: string): string {
    return text;
  }

  formatAIResponse(text: string): string {
    if (!text) return '';
    
    // Format the AI response for better display
    let formattedText = text;
    
    // Replace section headers with styled headers
    formattedText = formattedText.replace(/^(SUMMARY|Portfolio Insights|Risk Assessment|Diversification Analysis|Goal Alignment|Market Context|Specific Recommendations|Next Steps):/gm, 
      '<h5 style="color: #2c3e50; margin-top: 20px; margin-bottom: 10px; border-bottom: 2px solid #3498db; padding-bottom: 5px;">$1</h5>');
    
    // Replace bullet points with styled lists
    formattedText = formattedText.replace(/^[•\-\*]\s+/gm, '<li style="margin-bottom: 5px;">');
    formattedText = formattedText.replace(/(<li[^>]*>.*?)(?=\n|$)/g, '$1</li>');
    
    // Wrap lists in ul tags
    formattedText = formattedText.replace(/(<li[^>]*>.*?<\/li>)/gs, '<ul style="margin: 10px 0; padding-left: 20px;">$1</ul>');
    
    // Replace newlines with <br> tags
    formattedText = formattedText.replace(/\n/g, '<br>');
    
    // Add some basic styling
    formattedText = `<div style="line-height: 1.6; color: #34495e;">${formattedText}</div>`;
    
    return formattedText;
  }

  getAIAdjustments(): any[] {
    if (!this.currentInsight?.structuredData) {
      return [];
    }
    
    try {
      const structuredData = JSON.parse(this.currentInsight.structuredData);
      const adjustments = structuredData.scoreAdjustments;
      
      if (!adjustments) {
        return [];
      }
      
      const result = [];
      
      if (adjustments.risk) {
        result.push({
          type: 'Risk',
          adjustment: adjustments.risk.adjustment,
          reasoning: adjustments.risk.reasoning
        });
      }
      
      if (adjustments.diversification) {
        result.push({
          type: 'Diversification',
          adjustment: adjustments.diversification.adjustment,
          reasoning: adjustments.diversification.reasoning
        });
      }
      
      if (adjustments.goalAlignment) {
        result.push({
          type: 'Goal Alignment',
          adjustment: adjustments.goalAlignment.adjustment,
          reasoning: adjustments.goalAlignment.reasoning
        });
      }
      
      return result;
    } catch (error) {
      console.error('Error parsing AI adjustments:', error);
      return [];
    }
  }

  getDebugInfo(): any {
    if (!this.currentInsight?.debugInfo) {
      return null;
    }
    
    try {
      return JSON.parse(this.currentInsight.debugInfo);
    } catch (error) {
      console.error('Error parsing debug info:', error);
      return null;
    }
  }

  formatDebugInfo(): string {
    const debugInfo = this.getDebugInfo();
    if (!debugInfo) {
      return 'No debug information available';
    }
    
    let debugText = '=== DEBUG INFORMATION ===\n\n';
    
    // Portfolio Statistics
    if (debugInfo.portfolioStats) {
      const stats = debugInfo.portfolioStats;
      debugText += 'PORTFOLIO STATISTICS:\n';
      debugText += `Total Value: $${stats.totalValue?.toLocaleString()}\n`;
      debugText += `Holdings Count: ${stats.holdingsCount}\n`;
      debugText += `Client Risk Tolerance: ${stats.clientRiskTolerance}\n`;
      debugText += `Client Goal: ${stats.clientGoal}\n`;
      debugText += `Time Horizon: ${stats.clientTimeHorizon} years\n\n`;
      
      // Sector Distribution
      if (stats.sectorDistribution) {
        debugText += 'SECTOR DISTRIBUTION:\n';
        Object.entries(stats.sectorDistribution).forEach(([sector, value]) => {
          const percentage = (Number(value) / Number(stats.totalValue) * 100).toFixed(1);
          debugText += `${sector}: $${Number(value).toLocaleString()} (${percentage}%)\n`;
        });
        debugText += '\n';
      }
      
      // Top Holdings
      if (stats.topHoldings) {
        debugText += 'TOP 5 HOLDINGS:\n';
        stats.topHoldings.forEach((holding: any, index: number) => {
          debugText += `${index + 1}. ${holding.symbol} (${holding.sector})\n`;
          debugText += `   Shares: ${holding.shares.toLocaleString()}\n`;
          debugText += `   Price: $${holding.pricePerShare}\n`;
          debugText += `   Value: $${Number(holding.value).toLocaleString()} (${holding.percentage.toFixed(1)}%)\n`;
        });
        debugText += '\n';
      }
    }
    
    // Base scores
    debugText += 'BASE MATHEMATICAL SCORES:\n';
    debugText += `Risk: ${debugInfo.baseRiskScore}/10\n`;
    debugText += `Diversification: ${debugInfo.baseDiversificationScore}%\n`;
    debugText += `Goal Alignment: ${debugInfo.baseGoalAlignmentScore}%\n\n`;
    
    // Final scores
    debugText += 'FINAL AI-ENHANCED SCORES:\n';
    debugText += `Risk: ${debugInfo.finalRiskScore}/10\n`;
    debugText += `Diversification: ${debugInfo.finalDiversificationScore}%\n`;
    debugText += `Goal Alignment: ${debugInfo.finalGoalAlignmentScore}%\n\n`;
    
    // AI adjustments
    if (debugInfo.aiAdjustments) {
      debugText += 'AI ADJUSTMENTS:\n';
      if (debugInfo.aiAdjustments.risk) {
        debugText += `Risk: ${debugInfo.aiAdjustments.risk.adjustment} (${debugInfo.aiAdjustments.risk.reasoning})\n`;
      }
      if (debugInfo.aiAdjustments.diversification) {
        debugText += `Diversification: ${debugInfo.aiAdjustments.diversification.adjustment} (${debugInfo.aiAdjustments.diversification.reasoning})\n`;
      }
      if (debugInfo.aiAdjustments.goalAlignment) {
        debugText += `Goal Alignment: ${debugInfo.aiAdjustments.goalAlignment.adjustment} (${debugInfo.aiAdjustments.goalAlignment.reasoning})\n`;
      }
      debugText += '\n';
    }
    
    // Raw AI text
    if (this.currentInsight?.aiGeneratedText) {
      debugText += 'RAW AI GENERATED TEXT:\n';
      debugText += '=====================================\n';
      debugText += this.currentInsight.aiGeneratedText;
      debugText += '\n=====================================\n';
    }
    
    return debugText;
  }

  /**
   * Format insight date for display without timezone conversion
   * Handles both backend format and ISO strings
   */
  formatInsightDate(dateString: string): string {
    if (!dateString) return '';
    
    try {
      // If it's already an ISO string (has 'Z' or timezone), we need to handle it differently
      if (dateString.includes('Z') || dateString.includes('+')) {
        const date = new Date(dateString);
        return date.toLocaleString('en-US', {
          month: 'short',
          day: 'numeric',
          year: 'numeric',
          hour: 'numeric',
          minute: '2-digit',
          hour12: true
        });
      }
      
      // Parse the backend date string as local time (format: "2024-01-15T09:33:25")
      const [datePart, timePart] = dateString.split('T');
      if (!datePart || !timePart) {
        return dateString;
      }
      
      const [year, month, day] = datePart.split('-').map(Number);
      const [hour, minute, second] = timePart.split(':').map(Number);
      
      // Create date in local timezone
      const date = new Date(year, month - 1, day, hour, minute, second);
      
      // Format for display
      return date.toLocaleString('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric',
        hour: 'numeric',
        minute: '2-digit',
        hour12: true
      });
    } catch (error) {
      console.error('Error formatting date:', error);
      return dateString;
    }
  }

  getInsightSections(): any[] {
    if (!this.currentInsight?.structuredInsight?.sections) {
      return [];
    }
    
    // Debug: Log available sections
    console.log('=== INSIGHT SECTIONS DEBUG ===');
    console.log('Available sections:', Object.keys(this.currentInsight.structuredInsight.sections));
    console.log('Section-specific analysis:', Object.keys(this.currentInsight.structuredInsight.sectionSpecificAnalysis || {}));
    console.log('Raw sections data:', this.currentInsight.structuredInsight.sections);
    console.log('================================');
    
    const sections = Object.entries(this.currentInsight.structuredInsight.sections)
      .filter(([key, section]) => key !== 'asset_recommendations') // Filter out asset recommendations from tabs
      .map(([key, section]) => ({
        key,
        ...section
      }));
    
    // Set the first section as active if no active tab is set
    if (sections.length > 0 && !this.activeSectionTab) {
      this.activeSectionTab = sections[0].key;
    }
    
    return sections;
  }

  setActiveSectionTab(sectionKey: string): void {
    this.activeSectionTab = sectionKey;
  }

  getSectionSpecificAnalysis(sectionKey: string): string {
    if (!this.currentInsight?.structuredInsight?.sectionSpecificAnalysis) {
      return '';
    }
    
    const analysis = this.currentInsight.structuredInsight.sectionSpecificAnalysis[sectionKey] || '';
    
    // Debug: Log section-specific analysis
    console.log(`=== SECTION ANALYSIS DEBUG ===`);
    console.log(`Section key: ${sectionKey}`);
    console.log(`Analysis content: ${analysis.substring(0, 100)}...`);
    console.log(`Available keys: ${Object.keys(this.currentInsight.structuredInsight.sectionSpecificAnalysis)}`);
    console.log(`================================`);
    
    return analysis;
  }

  getAssetRecommendations(): any[] {
    if (!this.currentInsight?.structuredInsight?.assetRecommendations) {
      return [];
    }
    
    return this.currentInsight.structuredInsight.assetRecommendations || [];
  }

  getPriorityValidationMessage(rec: any): string | null {
    // Generic validation logic for frontend display
    const category = rec.category?.toLowerCase() || '';
    const priority = rec.priority?.toLowerCase() || '';
    
    // Check for potential priority mismatches based on category types
    if (category.includes('international') && priority === 'low') {
      return 'Consider: International exposure is often important for diversification';
    }
    
    if (category.includes('bond') && priority === 'low') {
      return 'Consider: Bonds provide stability for conservative goals';
    }
    
    if (category.includes('dividend') && priority === 'low') {
      return 'Consider: Dividend focus can help with income goals';
    }
    
    if (category.includes('real estate') && priority === 'low') {
      return 'Consider: Real estate can provide diversification and income';
    }
    
    if (category.includes('small cap') && priority === 'low') {
      return 'Consider: Small cap exposure can enhance growth potential';
    }
    
    if (category.includes('technology') && priority === 'low') {
      return 'Consider: Technology sector is often a key growth driver';
    }
    
    if (category.includes('healthcare') && priority === 'low') {
      return 'Consider: Healthcare sector provides defensive characteristics';
    }
    
    if (category.includes('financial') && priority === 'low') {
      return 'Consider: Financial sector can provide value and income';
    }
    
    if (category.includes('commodity') && priority === 'low') {
      return 'Consider: Commodities can provide inflation protection';
    }
    
    if (category.includes('mutual fund') && priority === 'low') {
      return 'Consider: Mutual funds provide professional management and diversification';
    }
    
    if (category.includes('individual stock') && priority === 'low') {
      return 'Consider: Individual stocks can provide targeted exposure and growth potential';
    }
    
    if (category.includes('reit') && priority === 'low') {
      return 'Consider: REITs provide real estate exposure and income generation';
    }
    
    return null; // No validation message needed
  }

  extractActionItems(content: string): string[] {
    if (!content) return [];
    
    const actionItems: string[] = [];
    const lines = content.split('\n');
    
    for (const line of lines) {
      const trimmedLine = line.trim();
      
      // Look for action-oriented phrases
      if (trimmedLine && (
        trimmedLine.toLowerCase().includes('consider') ||
        trimmedLine.toLowerCase().includes('recommend') ||
        trimmedLine.toLowerCase().includes('suggest') ||
        trimmedLine.toLowerCase().includes('should') ||
        trimmedLine.toLowerCase().includes('need to') ||
        trimmedLine.toLowerCase().includes('action') ||
        trimmedLine.toLowerCase().includes('review') ||
        trimmedLine.toLowerCase().includes('monitor') ||
        trimmedLine.toLowerCase().includes('evaluate') ||
        trimmedLine.toLowerCase().includes('assess')
      )) {
        // Clean up the line and add to action items
        let actionItem = trimmedLine
          .replace(/^[-•*]\s*/, '') // Remove bullet points
          .replace(/^\d+\.\s*/, '') // Remove numbers
          .trim();
        
        if (actionItem && actionItem.length > 10 && actionItem.length < 200) {
          actionItems.push(actionItem);
        }
      }
    }
    
    // Limit to 5 action items
    return actionItems.slice(0, 5);
  }
  
  refreshData(): void {
    // Refresh all data
    this.loadUserData();
    this.loadPortfolios();
    if (this.selectedClientId) {
      this.loadPortfoliosForClient();
    }
    this.updatePortfolioOverview();
  }
  
  clearData(): void {
    // Clear all data
  }

  closeInsight(): void {
    this.currentInsight = null;
    this.activeSectionTab = '';
    this.showDebugInfo = false;
  }

  // Portfolio carousel methods
  startPortfolioCarousel(): void {
    if (this.portfolios.length <= 1) return;
    
    this.stopPortfolioCarousel();
    this.portfolioCarouselInterval = setInterval(() => {
      this.nextPortfolio();
    }, 10000); // 10 seconds
  }

  stopPortfolioCarousel(): void {
    if (this.portfolioCarouselInterval) {
      clearInterval(this.portfolioCarouselInterval);
      this.portfolioCarouselInterval = null;
    }
  }

  nextPortfolio(): void {
    if (this.portfolios.length <= 1) return;
    
    this.currentPortfolioIndex = (this.currentPortfolioIndex + 1) % this.portfolios.length;
    this.updateCarouselPortfolio();
  }

  previousPortfolio(): void {
    if (this.portfolios.length <= 1) return;
    
    this.currentPortfolioIndex = this.currentPortfolioIndex === 0 
      ? this.portfolios.length - 1 
      : this.currentPortfolioIndex - 1;
    this.updateCarouselPortfolio();
  }

  selectPortfolioByIndex(index: number): void {
    if (index >= 0 && index < this.portfolios.length) {
      this.currentPortfolioIndex = index;
      this.updateCarouselPortfolio();
      this.restartCarousel();
    }
  }

  private updateCarouselPortfolio(): void {
    if (this.portfolios.length > 0) {
      const portfolio = this.portfolios[this.currentPortfolioIndex];
      this.selectedPortfolioId = portfolio.id.toString();
      this.selectedPortfolio = portfolio;
      this.updatePortfolioOverview();
      this.loadPortfolioSpecificNews(portfolio.id.toString()); // Load portfolio-specific news
    }
  }

  private calculateSectorAnalysis(holdings: any[]): void {
    const sectorMap = new Map<string, number>();
    
    holdings.forEach(holding => {
      const sector = holding.sector || 'Unknown';
      const value = holding.shares * holding.pricePerShare;
      sectorMap.set(sector, (sectorMap.get(sector) || 0) + value);
    });
    
    // Convert to array and sort by value
    const sectorArray = Array.from(sectorMap.entries())
      .map(([sector, value]) => ({ sector, value, percentage: (value / this.totalValue) * 100 }))
      .sort((a, b) => b.value - a.value);
    
    this.sectorAnalysis = {
      sectors: sectorArray,
      totalValue: this.totalValue
    };
  }

  private restartCarousel(): void {
    this.stopPortfolioCarousel();
    this.startPortfolioCarousel();
  }

  getVisiblePortfolios(): any[] {
    return this.portfolios.slice(0, 5);
  }

  hasMorePortfolios(): boolean {
    return this.portfolios.length > 5;
  }

  // Market News Methods
  loadMarketNews(): void {
    // Check if we need to update news (30 minutes cache)
    const thirtyMinutesAgo = new Date(Date.now() - 30 * 60 * 1000);
    
    if (this.lastNewsUpdate && this.lastNewsUpdate > thirtyMinutesAgo && this.marketNews.length > 0) {
      // Use cached news
      return;
    }
    
    this.http.get<any>('http://localhost:8000/market-news')
      .subscribe({
        next: (response) => {
          // Convert structured news data to the expected format
          this.marketNews = response.headlines.map((newsItem: any) => ({
            headline: newsItem.headline || newsItem,
            summary: newsItem.summary || newsItem.headline || newsItem,
            url: newsItem.url || 'https://finnhub.io' // Use actual article URL or fallback
          }));
          this.lastNewsUpdate = new Date();
          console.log('Loaded market news:', this.marketNews);
        },
        error: (error) => {
          console.error('Error loading market news:', error);
          // Set default news if API fails
          this.marketNews = [
            { headline: 'Market Update', summary: 'Financial markets continue to show resilience amid economic data releases.' },
            { headline: 'Investment Trends', summary: 'Investors focus on sector rotation and portfolio diversification strategies.' },
            { headline: 'Economic Outlook', summary: 'Analysts project steady growth with continued market volatility expected.' }
          ];
        }
      });
  }

  loadPortfolioSpecificNews(portfolioId: string): void {
    console.log(`🔄 Loading portfolio-specific news for portfolio: ${portfolioId}`);
    
    // Check if we have cached news for this portfolio (30 minutes cache)
    const thirtyMinutesAgo = new Date(Date.now() - 30 * 60 * 1000);
    const cachedData = this.portfolioNewsCache.get(portfolioId);
    
    if (cachedData && cachedData.timestamp > thirtyMinutesAgo && cachedData.news.length > 0) {
      // Use cached news for this portfolio
      this.marketNews = cachedData.news;
      this.lastNewsUpdate = cachedData.timestamp;
      console.log(`✅ Using cached news for portfolio ${portfolioId}:`, this.marketNews);
      return;
    }
    
    console.log(`📡 Fetching fresh news for portfolio ${portfolioId}...`);
    
    // First, test if AI microservice is responding
    this.http.get<any>('http://localhost:8000/market-news').pipe(
      timeout(5000),
      catchError((error) => {
        console.error(`❌ AI microservice not responding:`, error);
        this.marketNews = [
          { headline: 'Service Unavailable', summary: 'AI microservice is not responding. Please check if it is running.' },
          { headline: 'Portfolio Update', summary: 'Portfolio analysis shows continued market opportunities and strategic positioning.' },
          { headline: 'Investment Strategy', summary: 'Focus on diversification and risk management for optimal portfolio performance.' }
        ];
        return of({ headlines: [] });
      })
    ).subscribe({
      next: (response) => {
        console.log(`✅ AI microservice is responding, proceeding with portfolio news...`);
        // Continue with portfolio-specific news loading
        this.loadPortfolioHoldingsAndNews(portfolioId);
      }
    });
  }

  private loadPortfolioHoldingsAndNews(portfolioId: string): void {
    // First, get the portfolio holdings
    console.log(`📊 Fetching holdings for portfolio ${portfolioId}...`);
    this.http.get<any[]>(`http://localhost:8080/api/portfolios/${portfolioId}/holdings`)
      .subscribe({
        next: (holdings) => {
          console.log(`📋 Raw holdings data:`, holdings);
          // Extract ticker symbols from holdings
          const tickers = holdings.map(holding => holding.ticker).filter(ticker => ticker);
          console.log(`🎯 Extracted tickers for portfolio ${portfolioId}:`, tickers);
          
          if (tickers.length === 0) {
            console.log(`⚠️ No tickers found for portfolio ${portfolioId}, using general news`);
            this.loadMarketNews();
            return;
          }
          
          // Load portfolio-specific news with holdings data
          this.loadNewsWithHoldings(portfolioId, tickers);
        },
        error: (error) => {
          console.error(`❌ Error loading holdings for portfolio ${portfolioId}:`, error);
          // Fall back to general market news if we can't get holdings
          console.log(`🔄 Falling back to general market news`);
          this.loadMarketNews();
        }
      });
  }

  private loadNewsWithHoldings(portfolioId: string, tickers: string[]): void {
    console.log(`📰 Loading news with holdings for portfolio ${portfolioId}:`, tickers);
    
    // Prepare the request payload
    const requestPayload = {
      portfolio_id: portfolioId,
      holdings: tickers
    };
    
    console.log(`📤 Sending request to AI microservice:`, requestPayload);
    
    // Load portfolio-specific news with holdings
    this.http.post<any>(`http://localhost:8000/portfolio-news`, requestPayload, {
      headers: { 'Content-Type': 'application/json' }
    }).pipe(
      // Add timeout to prevent hanging requests
      timeout(10000), // 10 second timeout
      catchError((error) => {
        console.error(`⏰ Timeout or error for portfolio ${portfolioId}:`, error);
        return of({ headlines: [] }); // Return empty response to trigger error handling
      })
    )
      .subscribe({
        next: (response) => {
          console.log(`📥 Received response from AI microservice:`, response);
          
          // Convert structured news data to the expected format
          const portfolioNews = response.headlines.map((newsItem: any) => ({
            headline: newsItem.headline || newsItem,
            summary: newsItem.summary || newsItem.headline || newsItem,
            url: newsItem.url || 'https://finnhub.io' // Use actual article URL or fallback
          }));
          
          // Cache the news for this portfolio
          this.portfolioNewsCache.set(portfolioId, {
            news: portfolioNews,
            timestamp: new Date()
          });
          
          this.marketNews = portfolioNews;
          this.lastNewsUpdate = new Date();
          console.log(`✅ Successfully loaded portfolio-specific news for ${portfolioId} with holdings ${tickers}:`, this.marketNews);
        },
        error: (error) => {
          console.error(`❌ Error loading portfolio news for ${portfolioId}:`, error);
          console.log(`🔄 Setting fallback news due to error`);
          // Set default news if API fails
          this.marketNews = [
            { headline: 'Portfolio Update', summary: 'Portfolio analysis shows continued market opportunities and strategic positioning.' },
            { headline: 'Investment Strategy', summary: 'Focus on diversification and risk management for optimal portfolio performance.' },
            { headline: 'Market Analysis', summary: 'Current market conditions support strategic portfolio adjustments and rebalancing.' }
          ];
        }
      });
  }

  ngOnDestroy(): void {
    this.stopPortfolioCarousel();
  }
} 