import { test, expect } from '@playwright/test';
import { RequirementPage } from '../../pages/requirement.page';

test.describe('需求清单', () => {
  let reqPage: RequirementPage;

  test.beforeEach(async ({ page }) => {
    reqPage = new RequirementPage(page);
    await reqPage.goto();
  });

  test('页面加载-标题可见', async ({ page }) => {
    await expect(page.locator('.page-header h3')).toHaveText('需求管理');
  });

  test('提交需求按钮可见', async () => {
    await expect(reqPage.createButton).toBeVisible();
  });

  test('切换到全部需求', async () => {
    await reqPage.switchToAllRequirements();
  });

  test('切换到我的需求', async () => {
    await reqPage.switchToMyRequirements();
  });

  test('搜索栏元素可见', async () => {
    await expect(reqPage.searchButton).toBeVisible();
    await expect(reqPage.resetButton).toBeVisible();
  });

  test('需求表格可见', async () => {
    await expect(reqPage.requirementTable).toBeVisible();
  });

  test('打开提交需求对话框', async ({ page }) => {
    await reqPage.createButton.click();
    // RequirementFormDialog 应出现
    await expect(page.getByRole('dialog')).toBeVisible();
  });

  test('重置搜索条件', async ({ page }) => {
    await reqPage.resetButton.click();
    await page.waitForLoadState('networkidle');
  });
});
