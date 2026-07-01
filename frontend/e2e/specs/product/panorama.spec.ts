import { test, expect } from '@playwright/test';
import { DataListPage } from '../../pages/data-list.page';
import { ROUTES } from '../../fixtures/test-data';

test.describe('产品全景图', () => {
  let dataListPage: DataListPage;

  test.beforeEach(async ({ page }) => {
    dataListPage = new DataListPage(page);
    await dataListPage.navigateTo(ROUTES.dashboard);
    // 选择版本
    const versionRow = page.locator('.version-pick .el-table__body .el-table__row').first();
    if (await versionRow.isVisible()) {
      await versionRow.click();
      await page.waitForLoadState('networkidle');
    }
  });

  test('全景图页签可见', async ({ page }) => {
    await expect(dataListPage.panoramaTab).toBeVisible();
  });

  test('切换到全景图页签', async ({ page }) => {
    await dataListPage.panoramaTab.click();
    // 全景图内容区域应可见
    await expect(page.locator('.panorama-tab, .panorama-wrapper')).toBeVisible();
  });
});
