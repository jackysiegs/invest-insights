import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { InsightSection } from '../../services/insights.service';

@Component({
  selector: 'app-insight-section',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './insight-section.component.html',
  styleUrl: './insight-section.component.scss'
})
export class InsightSectionComponent {
  @Input() section!: InsightSection;
  @Input() sectionKey: string = '';
  
  isExpanded: boolean = true;

  getRiskLevelColor(): string {
    switch (this.section.riskLevel) {
      case 'high': return '#dc2626';
      case 'medium': return '#f59e0b';
      case 'low': return '#10b981';
      default: return '#6b7280';
    }
  }

  getRiskLevelIcon(): string {
    switch (this.section.riskLevel) {
      case 'high': return '⚠️';
      case 'medium': return '⚡';
      case 'low': return '✅';
      default: return 'ℹ️';
    }
  }

  toggleExpanded(): void {
    this.isExpanded = !this.isExpanded;
  }

  formatContent(content: string): string {
    // Convert line breaks to HTML and highlight key terms
    return content
      .replace(/\n/g, '<br>')
      .replace(/(\d+\.?\d*%)/g, '<strong>$1</strong>')
      .replace(/(\$\d+,?\d*)/g, '<strong>$1</strong>');
  }
} 