import { test, expect } from '@playwright/test';
import { VersionManagePage } from '../../pages/version-manage.page';

test.describe('版本管理', () => {
  let versionPage: VersionManagePage;

  test.beforeEach(async ({ page }) => {
    versionPage = new VersionManagePage(page);
    await versionPage.goto();
  });

  test('页面加载-标题可见', async ({ page }) => {
    await expect(page.locator('.page-header h3')).toHaveText('版本管理');
  });

  test('创建新版本按钮可见', async ({ page }) => {
    // 如果已有草稿版本，按钮可能disabled
    await expect(versionPage.createVersionButton).toBeVisible();
  });

  test('版本表格可见', async () => {
    await expect(versionPage.versionTable).toBeVisible();
  });

  test('表格有版本数据', async () => {
    const count = await versionPage.getVersionCount();
    expect(count).toBeGreaterThan(0);
  });

  test('状态栏显示当前版本', async () => {
    await expect(versionPage.statusBar).toBeVisible();
  });
});
