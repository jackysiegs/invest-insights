import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { InsightsService, InvestmentInsight } from '../services/insights.service';
import { InsightSectionComponent } from '../components/insight-section/insight-section.component';

@Component({
  selector: 'app-portfolio-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, InsightSectionComponent],
  templateUrl: './portfolio-detail.component.html',
  styleUrl: './portfolio-detail.component.scss'
})
export class PortfolioDetailComponent implements OnInit {
  // Portfolio data
  portfolio: any = null;
  holdings: any[] = [];
  portfolioId: string = '';
  
  // Portfolio metrics
  totalValue: number = 0; // Total invested value (calculated from holdings)
  totalPortfolioValue: number = 0; // Total portfolio value (including cash)
  cashAmount: number = 0; // Cash amount (total portfolio - invested)
  holdingsCount: number = 0;
  averageBeta: number = 0;
  totalDividendYield: number = 0;
  
  // Sector breakdown
  sectorBreakdown: any[] = [];
  assetTypeBreakdown: any[] = [];
  
  // Loading states
  isLoading: boolean = false;
  isLoadingHoldings: boolean = false;
  
  // UI state
  activeSection: string = 'overview';

  // Insights
  recentInsights: InvestmentInsight[] = [];
  currentInsight: InvestmentInsight | null = null;
  analysisPreferences: string = '';
  isGeneratingInsight: boolean = false;

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private http: HttpClient,
    private insightsService: InsightsService
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      this.portfolioId = params['id'];
      if (this.portfolioId) {
        // Clear any existing insight data to prevent stale data
        this.currentInsight = null;
        this.recentInsights = [];
        
        this.loadPortfolioDetails();
        this.loadPortfolioHoldings();
        this.loadInsightsForPortfolio();
      }
    });
  }

  loadPortfolioDetails(): void {
    this.isLoading = true;
    this.http.get<any>(`http://localhost:8080/api/portfolios/${this.portfolioId}`)
      .subscribe({
        next: (portfolio) => {
          this.portfolio = portfolio;
          // Store the total portfolio value (including cash)
          this.totalPortfolioValue = portfolio.totalValue || 0;
          this.isLoading = false;
        },
        error: (error) => {
          console.error('Error loading portfolio details:', error);
          this.isLoading = false;
        }
      });
  }

  loadPortfolioHoldings(): void {
    this.isLoadingHoldings = true;
    this.http.get<any[]>(`http://localhost:8080/api/portfolios/${this.portfolioId}/holdings`)
      .subscribe({
        next: (holdings) => {
          this.holdings = holdings;
          this.holdingsCount = holdings.length;
          // Calculate total value from holdings (same as backend)
          this.calculateTotalValue();
          this.calculatePortfolioMetrics();
          this.calculateSectorBreakdown();
          this.calculateAssetTypeBreakdown();
          this.isLoadingHoldings = false;
        },
        error: (error) => {
          console.error('Error loading portfolio holdings:', error);
          this.holdings = [];
          this.holdingsCount = 0;
          this.isLoadingHoldings = false;
        }
      });
  }

  calculateTotalValue(): void {
    // Calculate total invested value from holdings (same as backend logic)
    this.totalValue = this.holdings.reduce((total, holding) => {
      return total + (holding.shares * holding.pricePerShare);
    }, 0);
    
    // Calculate cash amount (difference between total portfolio and invested)
    this.cashAmount = this.totalPortfolioValue - this.totalValue;
  }

  calculatePortfolioMetrics(): void {
    if (this.holdings.length === 0) return;

    let totalBeta = 0;
    let totalDividendIncome = 0;
    let totalWeightedBeta = 0;
    let totalWeightedDividend = 0;

    this.holdings.forEach(holding => {
      const holdingValue = holding.shares * holding.pricePerShare;
      const weight = holdingValue / this.totalValue;
      
      if (holding.beta) {
        totalWeightedBeta += weight * holding.beta;
      }
      
      if (holding.dividendYield) {
        totalWeightedDividend += weight * holding.dividendYield;
      }
    });

    this.averageBeta = totalWeightedBeta;
    this.totalDividendYield = totalWeightedDividend;
  }

  calculateSectorBreakdown(): void {
    const sectorMap = new Map<string, number>();
    
    this.holdings.forEach(holding => {
      const sector = holding.sector || 'Unknown';
      const holdingValue = holding.shares * holding.pricePerShare;
      const currentValue = sectorMap.get(sector) || 0;
      sectorMap.set(sector, currentValue + holdingValue);
    });

    this.sectorBreakdown = Array.from(sectorMap.entries())
      .map(([sector, value]) => ({
        sector,
        value,
        percentage: (value / this.totalValue) * 100
      }))
      .sort((a, b) => b.value - a.value);
  }

  calculateAssetTypeBreakdown(): void {
    const assetTypeMap = new Map<string, number>();
    
    this.holdings.forEach(holding => {
      const assetType = holding.assetType || 'Unknown';
      const holdingValue = holding.shares * holding.pricePerShare;
      const currentValue = assetTypeMap.get(assetType) || 0;
      assetTypeMap.set(assetType, currentValue + holdingValue);
    });

    this.assetTypeBreakdown = Array.from(assetTypeMap.entries())
      .map(([assetType, value]) => ({
        assetType,
        value,
        percentage: (value / this.totalValue) * 100
      }))
      .sort((a, b) => b.value - a.value);
  }

  getHoldingValue(holding: any): number {
    return holding.shares * holding.pricePerShare;
  }

  getHoldingWeight(holding: any): number {
    const holdingValue = this.getHoldingValue(holding);
    return (holdingValue / this.totalValue) * 100;
  }

  setActiveSection(section: string): void {
    this.activeSection = section;
  }

  goBack(): void {
    // Get the client info from query parameters
    this.route.queryParams.subscribe(queryParams => {
      const clientId = queryParams['clientId'];
      const clientName = queryParams['clientName'];
      
      this.router.navigate(['/dashboard'], { 
        queryParams: { 
          tab: 'portfolios',
          clientId: clientId,
          clientName: clientName
        } 
      });
    });
  }

  loadInsightsForPortfolio(): void {
    if (!this.portfolioId) {
      this.recentInsights = [];
      this.currentInsight = null;
      return;
    }

    this.insightsService.getInsightsByPortfolio(this.portfolioId).subscribe({
      next: (insights) => {
        // Sort insights in descending order (newest first) - using string comparison to avoid timezone issues
        console.log('=== SORTING DEBUG ===');
        insights.forEach((insight, index) => {
          console.log(`Before sort - Insight ${index}:`, {
            id: insight.id,
            createdAt: insight.createdAt,
            createdAtType: typeof insight.createdAt
          });
        });
        
        this.recentInsights = insights.sort((a, b) => {
          // Use string comparison instead of Date objects to avoid timezone conversion
          return b.createdAt.localeCompare(a.createdAt);
        });
        
        console.log('=== AFTER SORTING ===');
        this.recentInsights.forEach((insight, index) => {
          console.log(`After sort - Insight ${index}:`, {
            id: insight.id,
            createdAt: insight.createdAt
          });
        });
        console.log('================================');
        
        // Set the most recent insight as current if available
        if (this.recentInsights.length > 0) {
          this.currentInsight = this.recentInsights[0];
        }
        
        // Store original date strings to prevent timezone conversion issues
        this.recentInsights.forEach(insight => {
          if (typeof insight.createdAt === 'string') {
            // Store the original backend date string as a property to prevent re-parsing
            (insight as any).originalCreatedAt = insight.createdAt;
            console.log(`Stored original date for insight ${insight.id}:`, (insight as any).originalCreatedAt);
          }
        });
        console.log('Loaded insights for portfolio:', insights);
        // Debug: Log the scores and dates for each insight
        insights.forEach((insight, index) => {
          console.log(`Insight ${index + 1}:`, {
            id: insight.id,
            createdAt: insight.createdAt,
            createdAtType: typeof insight.createdAt,
            riskScore: insight.riskScore,
            diversificationScore: insight.diversificationScore,
            goalAlignment: insight.goalAlignment
          });
        });
      },
      error: (error) => {
        console.error('Error loading insights for portfolio:', error);
        this.recentInsights = [];
      }
    });
  }

  generateInsight(): void {
    if (!this.portfolioId || this.isGeneratingInsight) {
      return;
    }

    this.isGeneratingInsight = true;
    
    // Get client ID from portfolio or use a default
    const clientId = this.portfolio?.client?.id?.toString() || '1';
    
    this.insightsService.generateInsight(
      this.portfolioId, 
      this.analysisPreferences || 'Provide a comprehensive portfolio analysis',
      clientId
    ).subscribe({
      next: (insight) => {
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
        
        // Debug: Log the final scores from the backend
        console.log('=== GENERATED INSIGHT DEBUG ===');
        console.log('Final Risk Score:', insight.riskScore);
        console.log('Final Diversification Score:', insight.diversificationScore);
        console.log('Final Goal Alignment:', insight.goalAlignment);
        console.log('================================');
        
        this.currentInsight = insight;
        // Add new insight to the beginning of the list
        this.recentInsights.unshift(insight);
        this.isGeneratingInsight = false;
        
        // Force refresh the insights list to ensure we have the latest data
        setTimeout(() => {
          this.loadInsightsForPortfolio();
        }, 1000);
      },
      error: (error) => {
        console.error('Error generating insight:', error);
        this.isGeneratingInsight = false;
      }
    });
  }

  addHolding(): void {
    // TODO: Implement add holding functionality
    console.log('Add holding to portfolio:', this.portfolioId);
  }

  viewInsight(insight: InvestmentInsight): void {
    // Debug: Log the raw insight data
    console.log('=== VIEW INSIGHT DEBUG ===');
    console.log('Original createdAt:', insight.createdAt);
    console.log('Original date string:', (insight as any).originalCreatedAt);
    console.log('Raw insight scores:', {
      riskScore: insight.riskScore,
      diversificationScore: insight.diversificationScore,
      goalAlignment: insight.goalAlignment
    });
    console.log('Structured data:', insight.structuredData);
    console.log('AI generated text:', insight.aiGeneratedText);
    
    // Parse structured data if not already present
    if (!insight.structuredInsight) {
      const parsed = this.insightsService.parseStructuredData(insight);
      if (parsed) {
        insight.structuredInsight = parsed;
        console.log('Parsed structured insight:', parsed);
      }
    }
    
    // Use original date string to prevent timezone conversion issues
    if ((insight as any).originalCreatedAt) {
      insight.createdAt = (insight as any).originalCreatedAt;
    }
    
    this.currentInsight = insight;
    console.log('Final current insight scores:', {
      riskScore: this.currentInsight.riskScore,
      diversificationScore: this.currentInsight.diversificationScore,
      goalAlignment: this.currentInsight.goalAlignment
    });
    console.log('================================');
  }

  getInsightSections(): any[] {
    if (!this.currentInsight?.structuredInsight?.sections) {
      return [];
    }
    
    return Object.entries(this.currentInsight.structuredInsight.sections).map(([key, section]) => ({
      key,
      ...section
    }));
  }

  /**
   * Format backend date string for display without timezone conversion
   * Backend format: "2024-01-15T09:33:25" (local time)
   */
  formatInsightDate(dateString: string): string {
    if (!dateString) return '';
    
    console.log('=== FORMAT DATE DEBUG ===');
    console.log('Input date string:', dateString);
    console.log('Input type:', typeof dateString);
    
    try {
      // If it's already an ISO string (has 'Z' or timezone), we need to handle it differently
      if (dateString.includes('Z') || dateString.includes('+')) {
        console.log('ISO string detected, parsing as UTC');
        const date = new Date(dateString);
        const result = date.toLocaleString('en-US', {
          month: 'short',
          day: 'numeric',
          year: 'numeric',
          hour: 'numeric',
          minute: '2-digit',
          hour12: true
        });
        console.log('ISO string result:', result);
        console.log('================================');
        return result;
      }
      
      // Parse the backend date string as local time (format: "2024-01-15T09:33:25")
      const [datePart, timePart] = dateString.split('T');
      if (!datePart || !timePart) {
        console.log('Invalid date format, returning as-is');
        return dateString;
      }
      
      const [year, month, day] = datePart.split('-').map(Number);
      const [hour, minute, second] = timePart.split(':').map(Number);
      
      console.log('Parsed components:', { year, month, day, hour, minute, second });
      
      // Create date in local timezone
      const date = new Date(year, month - 1, day, hour, minute, second);
      
      console.log('Created date object:', date);
      console.log('Date toISOString:', date.toISOString());
      
      // Format for display
      const result = date.toLocaleString('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric',
        hour: 'numeric',
        minute: '2-digit',
        hour12: true
      });
      
      console.log('Formatted result:', result);
      console.log('================================');
      
      return result;
    } catch (error) {
      console.error('Error formatting date:', error);
      return dateString;
    }
  }
} 