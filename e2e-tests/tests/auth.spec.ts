import { expect, test } from '@playwright/test';

const adminEmail = process.env.ADMIN_EMAIL ?? 'admin@loginsight.dev';
const adminPassword = process.env.ADMIN_PASSWORD ?? 'admin123';

test.describe('Authentication', () => {
  test('login page renders', async ({ page }) => {
    await page.goto('/login');
    await expect(page.getByRole('textbox', { name: /email/i })).toBeVisible();
    await expect(page.locator('input[type="password"]')).toBeVisible();
    await expect(page.getByRole('button', { name: /sign in|log in|login|submit/i })).toBeVisible();
  });

  test('invalid credentials show an error', async ({ page }) => {
    await page.goto('/login');
    await page.locator('input[type="email"], input[name="email"]').first().fill('wrong@example.com');
    await page.locator('input[type="password"]').first().fill('wrong-password');
    await page.getByRole('button', { name: /sign in|log in|login|submit/i }).click();

    const error = page.locator('.error-message, [role="alert"]').first();
    await expect(error).toBeVisible({ timeout: 8000 });
  });

  test('successful login redirects', async ({ page }) => {
    await page.goto('/login');
    await page.locator('input[type="email"], input[name="email"]').first().fill(adminEmail);
    await page.locator('input[type="password"]').first().fill(adminPassword);
    await page.getByRole('button', { name: /sign in|log in|login|submit/i }).click();

    await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 8000 });
    expect(page.url()).not.toContain('/login');
  });
});
