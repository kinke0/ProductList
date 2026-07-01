import { test, expect } from '@playwright/test';
import { LoginPage } from '../../pages/login.page';
import { TEST_USER } from '../../fixtures/test-data';

// 登录模块测试不使用 storageState，需要独立运行
test.use({ storageState: { cookies: [], origins: [] } });

test.describe('登录模块', () => {
  let loginPage: LoginPage;

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page);
    await loginPage.goto();
  });

  test('正确密码登录成功', async ({ page }) => {
    await loginPage.login(TEST_USER.username, TEST_USER.password);
    await page.waitForURL(/\/dashboard/, { timeout: 15000 });
    await expect(page.locator('.login-container')).not.toBeVisible();
    // 验证 token 已存储
    const token = await page.evaluate(() => localStorage.getItem('token'));
    expect(token).toBeTruthy();
  });

  test('错误密码登录失败', async ({ page }) => {
    await loginPage.login(TEST_USER.username, 'wrongpassword');
    // 应停留在登录页面
    await expect(page.locator('.login-container')).toBeVisible();
  });

  test('空用户名不提交', async ({ page }) => {
    await loginPage.login('', TEST_USER.password);
    // Element Plus 表单校验阻止提交，页面仍在登录页
    await expect(page.locator('.login-container')).toBeVisible();
  });

  test('空密码不提交', async ({ page }) => {
    await loginPage.login(TEST_USER.username, '');
    await expect(page.locator('.login-container')).toBeVisible();
  });

  test('打开注册对话框', async () => {
    await loginPage.openRegisterDialog();
    await expect(loginPage.registerDialog).toBeVisible();
    await expect(loginPage.registerUsernameInput).toBeVisible();
  });

  test('关闭注册对话框', async () => {
    await loginPage.openRegisterDialog();
    await loginPage.registerCancelButton.click();
    await expect(loginPage.registerDialog).not.toBeVisible();
  });
});
