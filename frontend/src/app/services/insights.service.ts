import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface InsightSection {
  title: string;
  content: string;
  keyMetrics: string[];
  riskLevel: 'low' | 'medium' | 'high' | 'neutral';
}

export interface AssetRecommendation {
  ticker: string;
  assetName: string;
  allocation: string;
  category: string;
  reasoning: string;
  priority: string;
  expectedImpact: string;
}

export interface StructuredInsight {
  riskScore: number;
  diversificationScore: number;
  goalAlignment: number;
  mainRecommendations: string[];
  sections: { [key: string]: InsightSection };
  rawResponse: string;
  qualitativeInsights?: { [key: string]: string };
  scoreAdjustments?: { [key: string]: { [key: string]: string } };
  sectionSpecificAnalysis?: { [key: string]: string };
  assetRecommendations?: AssetRecommendation[];
}

export interface InvestmentInsight {
  id?: number;
  summary: string;
  aiGeneratedText: string;
  createdAt: string;
  portfolioName?: string;
  structuredInsight?: StructuredInsight;
  riskScore?: number;
  diversificationScore?: number;
  goalAlignment?: number;
  structuredData?: string;
  debugInfo?: string;
}

@Injectable({
  providedIn: 'root'
})
export class InsightsService {

  constructor(private http: HttpClient) { }

  generateInsight(portfolioId: string, preferences: string, clientId: string): Observable<InvestmentInsight> {
    return this.http.post<InvestmentInsight>(`http://localhost:8080/api/insights/generate-portfolio/${portfolioId}?clientId=${clientId}`, {
      preferences: preferences
    });
  }

  getInsightsByPortfolio(portfolioId: string): Observable<InvestmentInsight[]> {
    // Add cache-busting parameter to ensure fresh data
    const timestamp = new Date().getTime();
    return this.http.get<InvestmentInsight[]>(`http://localhost:8080/api/insights/portfolio/${portfolioId}?_t=${timestamp}`);
  }

  getInsightsByClient(clientId: string): Observable<InvestmentInsight[]> {
    return this.http.get<InvestmentInsight[]>(`http://localhost:8080/api/insights/client/${clientId}`);
  }

  deleteInsight(insightId: number): Observable<void> {
    return this.http.delete<void>(`http://localhost:8080/api/insights/${insightId}`);
  }

  parseStructuredData(insight: InvestmentInsight): StructuredInsight | null {
    try {
      // First, try to parse from structuredData JSON string
      if (insight.structuredData) {
        const parsed = JSON.parse(insight.structuredData);
        return parsed as StructuredInsight;
      }
      
      // If no structuredData, try to reconstruct from individual fields
      if (insight.riskScore !== undefined || insight.diversificationScore !== undefined || insight.goalAlignment !== undefined) {
        return {
          riskScore: insight.riskScore || 5,
          diversificationScore: insight.diversificationScore || 50,
          goalAlignment: insight.goalAlignment || 50,
          mainRecommendations: [],
          sections: {},
          rawResponse: insight.aiGeneratedText || ""
        };
      }
      
      // If no structured data at all, try to parse from the AI text
      if (insight.aiGeneratedText) {
        // This would require the same parsing logic as the AI microservice
        // For now, return a basic structure
        return {
          riskScore: 5,
          diversificationScore: 50,
          goalAlignment: 50,
          mainRecommendations: [],
          sections: {},
          rawResponse: insight.aiGeneratedText
        };
      }
      
      return null;
    } catch (error) {
      console.error('Error parsing structured data:', error);
      return null;
    }
  }
} 