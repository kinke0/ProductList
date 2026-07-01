import { test, expect } from '@playwright/test';
import { RoleManagePage } from '../../pages/role-manage.page';

test.describe('权限套餐管理', () => {
  let rolePage: RoleManagePage;

  test.beforeEach(async ({ page }) => {
    rolePage = new RoleManagePage(page);
    await rolePage.goto();
  });

  test('页面加载-标题可见', async ({ page }) => {
    await expect(page.locator('.page-header h3')).toHaveText('权限套餐管理');
  });

  test('新增套餐按钮可见', async () => {
    await expect(rolePage.addButton).toBeVisible();
  });

  test('角色表格可见', async () => {
    await expect(rolePage.roleTable).toBeVisible();
  });

  test('打开新增权限套餐对话框', async ({ page }) => {
    await rolePage.addButton.click();
    await expect(rolePage.dialog).toBeVisible();
  });
});
