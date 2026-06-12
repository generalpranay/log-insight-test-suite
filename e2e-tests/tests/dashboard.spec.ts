import { expect, test } from '@playwright/test';

test.describe('Log-to-Insight dashboard', () => {
  test('dashboard loads', async ({ page }) => {
    await page.goto('/');
    await expect(page.getByRole('heading', { name: /log-to-insight/i })).toBeVisible();
  });

  test('log stream panel is visible', async ({ page }) => {
    await page.goto('/');
    const panel = page.locator('.log-stream-panel, [data-testid="log-stream"]').first();
    await expect(panel).toBeVisible({ timeout: 10000 });
  });

  test('real-time logs appear', async ({ page }) => {
    await page.goto('/');
    const firstEntry = page.locator('.log-entry').first();
    await expect(firstEntry).toBeVisible({ timeout: 15000 });
  });

  test('anomaly badge is shown', async ({ page }) => {
    await page.goto('/anomalies');
    const badge = page.locator('.anomaly-badge').first();
    await expect(badge).toBeVisible({ timeout: 10000 });
  });
});
