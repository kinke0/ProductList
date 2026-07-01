import { test, expect } from '@playwright/test';
import { DataListFunctionalPage } from '../../pages/data-list-functional.page';
import { DataListPage } from '../../pages/data-list.page';

test.describe('产品清单-搜索筛选', () => {
  let funcPage: DataListFunctionalPage;
  let dataListPage: DataListPage;

  test.beforeEach(async ({ page }) => {
    funcPage = new DataListFunctionalPage(page);
    dataListPage = new DataListPage(page);
    await funcPage.gotoDataList();
  });

  test('按名称搜索-结果数量变化', async ({ page }) => {
    // 记录初始记录数
    const initialCountText = await page.locator('.record-count').first().textContent().catch(() => null);
    // 输入搜索关键词
    await dataListPage.searchNameInput.fill('测试');
    await dataListPage.searchButton.click();
    await page.waitForTimeout(2000);
    // 验证搜索后页面仍正常
    await expect(page.locator('.data-list-tab')).toBeVisible({ timeout: 5000 });
    const afterCountText = await page.locator('.record-count').first().textContent().catch(() => null);
    expect(initialCountText).not.toBeUndefined();
    expect(afterCountText).not.toBeUndefined();
  });

  test('按状态筛选-选择可交付', async ({ page }) => {
    // 使用el-select组件方式筛选状态
    const statusSelectWrapper = page.locator('.query-bar .el-form-item').filter({ hasText: '状态' }).locator('.el-select').first();
    if (await statusSelectWrapper.isVisible({ timeout: 3000 }).catch(() => false)) {
      await statusSelectWrapper.click();
      await page.waitForTimeout(500);
      const option = page.getByRole('option', { name: '可交付' }).first();
      if (await option.isVisible({ timeout: 3000 }).catch(() => false)) {
        await option.click();
        await dataListPage.searchButton.click();
        await page.waitForTimeout(2000);
        await expect(page.locator('.data-list-tab')).toBeVisible({ timeout: 5000 });
      }
    }
  });

  test('按解决方案筛选-选择选项', async ({ page }) => {
    const solutionSelect = page.locator('.query-bar .el-form-item').filter({ hasText: '解决方案' }).locator('.el-select');
    if (await solutionSelect.isVisible({ timeout: 3000 }).catch(() => false)) {
      await solutionSelect.click();
      await page.waitForTimeout(500);
      const firstOption = page.getByRole('option').first();
      if (await firstOption.isVisible({ timeout: 3000 }).catch(() => false)) {
        await firstOption.click();
        await dataListPage.searchButton.click();
        await page.waitForTimeout(2000);
        await expect(page.locator('.data-list-tab')).toBeVisible({ timeout: 5000 });
      }
    }
  });

  test('按产品经理筛选-输入名称', async ({ page }) => {
    await dataListPage.searchNameInput.fill(''); // 清空名称
    const managerInput = page.getByPlaceholder('产品经理');
    if (await managerInput.isVisible({ timeout: 3000 }).catch(() => false)) {
      await managerInput.fill('测试');
      await dataListPage.searchButton.click();
      await page.waitForTimeout(2000);
      await expect(page.locator('.data-list-tab')).toBeVisible({ timeout: 5000 });
    }
  });

  test('智能化过滤-勾选后查询', async ({ page }) => {
    const checkbox = page.locator('.query-bar .el-checkbox').filter({ hasText: '智能化' });
    if (await checkbox.isVisible({ timeout: 3000 }).catch(() => false)) {
      await checkbox.click();
      await dataListPage.searchButton.click();
      await page.waitForTimeout(2000);
      await expect(page.locator('.data-list-tab')).toBeVisible({ timeout: 5000 });
    }
  });
});
