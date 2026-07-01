import { test, expect } from '@playwright/test';
import { BasePage } from '../../pages/base.page';
import { ROUTES } from '../../fixtures/test-data';

test.describe('图床管理', () => {
  let basePage: BasePage;

  test.beforeEach(async ({ page }) => {
    basePage = new BasePage(page);
    await basePage.navigateTo(ROUTES.imageGallery);
  });

  test('页面加载-标题可见', async ({ page }) => {
    await expect(page.locator('.page-header h3')).toContainText('图床');
  });

  test('图床内容区域可见', async ({ page }) => {
    // 验证图床核心区域可见
    await expect(page.locator('.image-gallery, .gallery-wrapper, .page')).toBeVisible();
  });
});
