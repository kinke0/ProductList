import { test, expect } from '@playwright/test';
import { UserManagePage } from '../../pages/user-manage.page';

test.describe('用户管理', () => {
  let userPage: UserManagePage;

  test.beforeEach(async ({ page }) => {
    userPage = new UserManagePage(page);
    await userPage.goto();
  });

  test('页面加载-标题可见', async ({ page }) => {
    await expect(page.locator('.page-header h3')).toHaveText('用户管理');
  });

  test('新增用户按钮可见', async () => {
    await expect(userPage.addUserButton).toBeVisible();
  });

  test('用户表格可见', async () => {
    await expect(userPage.userTable).toBeVisible();
  });

  test('表格有数据行', async () => {
    const count = await userPage.getRowCount();
    expect(count).toBeGreaterThan(0);
  });

  test('打开新增用户对话框', async ({ page }) => {
    await userPage.openAddDialog();
    await expect(userPage.dialog).toBeVisible();
  });

  test('点击编辑按钮', async ({ page }) => {
    const count = await userPage.getRowCount();
    if (count > 1) {
      await userPage.userTable.getByRole('row').nth(1).getByRole('button', { name: '编辑' }).click({ timeout: 5000 });
    }
  });

  test('点击操作日志按钮', async ({ page }) => {
    const count = await userPage.getRowCount();
    if (count > 1) {
      await userPage.userTable.getByRole('row').nth(1).getByRole('button', { name: '操作日志' }).click({ timeout: 5000 });
    }
  });
});
