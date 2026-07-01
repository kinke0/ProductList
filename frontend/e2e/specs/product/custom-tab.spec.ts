import { test, expect } from '@playwright/test';
import { DataListPage } from '../../pages/data-list.page';
import { ROUTES } from '../../fixtures/test-data';

test.describe('自定义清单', () => {
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

  test('添加清单按钮可见', async ({ page }) => {
    await expect(dataListPage.addListButton).toBeVisible();
  });

  test('点击添加清单按钮打开对话框', async ({ page }) => {
    await dataListPage.addListButton.click();
    await expect(page.getByRole('dialog', { name: '添加清单' })).toBeVisible();
  });

  test('自定义清单页签区域可见', async ({ page }) => {
    // 标签页栏应可见
    await expect(page.locator('.el-tabs')).toBeVisible();
  });
});
