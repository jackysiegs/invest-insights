import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { APP_VERSION } from '../version';

@Injectable({
  providedIn: 'root'
})
export class VersionService {
  private version: string = APP_VERSION; // Use build-time version
  private versionLoaded: boolean = false;

  constructor(private http: HttpClient) {
    // In development, try to load from package.json for live updates
    if (this.isDevelopment()) {
      this.loadVersionFromPackageJson();
    } else {
      this.versionLoaded = true;
    }
  }

  /**
   * Check if we're in development mode
   */
  private isDevelopment(): boolean {
    return window.location.hostname === 'localhost' || 
           window.location.hostname === '127.0.0.1' ||
           window.location.port === '4200';
  }

  /**
   * Load version from package.json (development only)
   */
  private loadVersionFromPackageJson(): void {
    this.http.get<any>('/package.json').pipe(
      map(packageJson => {
        this.version = packageJson.version || APP_VERSION;
        this.versionLoaded = true;
        console.log('Loaded version from package.json:', this.version);
      }),
      catchError(error => {
        console.warn('Could not load version from package.json, using build version:', error);
        this.versionLoaded = true;
        return of(null);
      })
    ).subscribe();
  }

  /**
   * Get the current application version
   * @returns Observable<string> - The version string
   */
  getVersion(): Observable<string> {
    if (this.versionLoaded) {
      return of(this.version);
    }

    // Wait for version to load
    return new Observable(observer => {
      const checkVersion = () => {
        if (this.versionLoaded) {
          observer.next(this.version);
          observer.complete();
        } else {
          setTimeout(checkVersion, 100);
        }
      };
      checkVersion();
    });
  }

  /**
   * Get version as a simple string (synchronous)
   * @returns string - The version string
   */
  getVersionSync(): string {
    return this.version;
  }

  /**
   * Get formatted version string for display
   * @returns Observable<string> - Formatted version string
   */
  getFormattedVersion(): Observable<string> {
    return this.getVersion().pipe(
      map(version => `v${version}`)
    );
  }
} 