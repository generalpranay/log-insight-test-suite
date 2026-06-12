import { expect, type Locator, type Page } from '@playwright/test';

/**
 * Page object for the Log-to-Insight dashboard, encapsulating the selectors and
 * interactions used by the dashboard specs.
 */
export class DashboardPage {
  readonly page: Page;
  readonly heading: Locator;
  readonly logStreamPanel: Locator;
  readonly logEntries: Locator;
  readonly anomalyBadge: Locator;

  constructor(page: Page) {
    this.page = page;
    this.heading = page.getByRole('heading', { name: /log-to-insight/i });
    this.logStreamPanel = page.locator('.log-stream-panel, [data-testid="log-stream"]').first();
    this.logEntries = page.locator('.log-entry');
    this.anomalyBadge = page.locator('.anomaly-badge').first();
  }

  async open(): Promise<void> {
    await this.page.goto('/');
  }

  async openAnomalies(): Promise<void> {
    await this.page.goto('/anomalies');
  }

  async expectLoaded(): Promise<void> {
    await expect(this.heading).toBeVisible();
  }

  async expectLogStreamVisible(timeout = 10000): Promise<void> {
    await expect(this.logStreamPanel).toBeVisible({ timeout });
  }

  async expectFirstLogEntry(timeout = 15000): Promise<void> {
    await expect(this.logEntries.first()).toBeVisible({ timeout });
  }
}
