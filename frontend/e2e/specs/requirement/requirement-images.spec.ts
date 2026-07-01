import { test, expect } from '@playwright/test';
import { BasePage } from '../../pages/base.page';
import { ROUTES } from '../../fixtures/test-data';

test.describe('需求图片', () => {
  let basePage: BasePage;

  test.beforeEach(async ({ page }) => {
    basePage = new BasePage(page);
    await basePage.navigateTo(ROUTES.requirementImages);
  });

  test('页面加载-标题可见', async ({ page }) => {
    await expect(page.locator('.page-header h3')).toContainText('需求图片');
  });

  test('需求图片画廊可见', async ({ page }) => {
    await expect(page.locator('.gallery-body')).toBeVisible({ timeout: 15000 });
  });
});
