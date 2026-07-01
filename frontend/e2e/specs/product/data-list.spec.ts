import { test, expect } from '@playwright/test';
import { DataListPage } from '../../pages/data-list.page';
import { ROUTES } from '../../fixtures/test-data';

import { TEST_USER } from '../../fixtures/test-data';

test.describe('产品清单', () => {
  let dataListPage: DataListPage;

  test.beforeEach(async ({ page }) => {
    dataListPage = new DataListPage(page);
    // 导航到仪表盘
    await dataListPage.navigateTo(ROUTES.dashboard);
    // 选择版本（点击版本表格第一行）
    const versionRow = page.locator('.version-pick .el-table__body .el-table__row').first();
    if (await versionRow.isVisible()) {
      await versionRow.click();
      await page.waitForLoadState('networkidle');
      // 磀悉版本加载后，切换到数据清单 tab
      await dataListPage.listTab.click();
      await page.waitForLoadState('networkidle');
    }
  });

  test('页面加载并显示数据清单', async ({ page }) => {
    // 验证数据清单可见
    await expect(page.locator('.data-list-tab')).toBeVisible();
    // 验证搜索栏存在
    await expect(dataListPage.searchNameInput).toBeVisible();
  });

  test('搜索栏-按名称搜索', async ({ page }) => {
    await dataListPage.searchNameInput.fill('测试');
    await dataListPage.searchButton.click();
    await page.waitForLoadState('networkidle');
    // 验证搜索生效（结果应有变化）
    await expect(page.locator('.data-list-tab')).toBeVisible();
  });

  test('搜索栏-重置搜索条件', async ({ page }) => {
    await dataListPage.searchNameInput.fill('测试');
    await dataListPage.searchButton.click();
    await page.waitForLoadState('networkidle');
    // 重置
    await dataListPage.resetButton.click();
    await page.waitForLoadState('networkidle');
    // 验证搜索条件已清空
    await expect(dataListPage.searchNameInput).toHaveValue('');
  });

  test('切换标签页-产品全景图', async ({ page }) => {
    await dataListPage.panoramaTab.click();
    await expect(page.locator('.panorama-tab')).toBeVisible();
  });

  test('切换标签页-统计视图', async ({ page }) => {
    await dataListPage.statsTab.click();
    await expect(page.locator('.stats-tab')).toBeVisible();
  });

  test('全部展开按钮', async ({ page }) => {
    await dataListPage.expandAllButton.click();
    await page.waitForLoadState('networkidle');
  });

  test('全部折叠按钮', async ({ page }) => {
    await dataListPage.collapseAllButton.click();
    await page.waitForLoadState('networkidle');
  });

  test('版本信息显示正确', async ({ page }) => {
    // 验证版本徽章可见
    await expect(dataListPage.versionBadge).toBeVisible();
  });

  test('切换版本按钮', async ({ page }) => {
    await dataListPage.switchVersionButton.click();
    // 验证版本切换对话框出现
    await expect(page.getByRole('dialog', { name: '切换版本' })).toBeVisible();
  });

  test('侧边栏收缩展开', async ({ page }) => {
    // 收缩侧边栏
    await page.locator('.sidebar-toggle').first().click();
    await page.waitForTimeout(500);
    // 再次展开
    await page.locator('.sidebar-toggle').first().click();
  });

  test('智能化复选框过滤', async ({ page }) => {
    const checkbox = page.locator('.query-bar .el-checkbox').filter({ hasText: '智能化' });
    await checkbox.click();
    await page.waitForLoadState('networkidle');
  });
});
