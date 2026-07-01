import { test as setup, expect } from '@playwright/test';
import { TEST_USER } from './test-data';

const authFile = 'e2e/.auth/user.json';

setup('authenticate', async ({ page }) => {
  await page.goto('/login');
  await page.getByPlaceholder('用户名').fill(TEST_USER.username);
  await page.getByPlaceholder('密码').fill(TEST_USER.password);
  await page.getByRole('button', { name: '登 录' }).click();
  await page.waitForURL(/\/dashboard/, { timeout: 15000 });
  await expect(page.locator('.login-container')).not.toBeVisible();
  await page.context().storageState({ path: authFile });
});
