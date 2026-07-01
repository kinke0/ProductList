import { BasePage } from './base.page';
import { Locator, Page } from '@playwright/test';
import { ROUTES } from '../fixtures/test-data';

export class LoginPage extends BasePage {
  readonly usernameInput: Locator;
  readonly passwordInput: Locator;
  readonly loginButton: Locator;
  readonly registerLink: Locator;
  readonly registerDialog: Locator;
  readonly registerUsernameInput: Locator;
  readonly registerPasswordInput: Locator;
  readonly registerConfirmPasswordInput: Locator;
  readonly registerNicknameInput: Locator;
  readonly registerSubmitButton: Locator;
  readonly registerCancelButton: Locator;

  constructor(page: Page) {
    super(page);
    this.usernameInput = page.getByPlaceholder('用户名');
    this.passwordInput = page.getByPlaceholder('密码');
    this.loginButton = page.getByRole('button', { name: '登 录' });
    this.registerLink = page.getByText('注册账号');

    // 注册对话框元素
    this.registerDialog = page.getByRole('dialog', { name: '注册账号' });
    this.registerUsernameInput = page.getByRole('dialog', { name: '注册账号' }).getByPlaceholder('请输入用户名');
    this.registerPasswordInput = page.getByRole('dialog', { name: '注册账号' }).getByPlaceholder('请输入密码');
    this.registerConfirmPasswordInput = page.getByRole('dialog', { name: '注册账号' }).getByPlaceholder('请再次输入密码');
    this.registerNicknameInput = page.getByRole('dialog', { name: '注册账号' }).getByPlaceholder('请输入姓名');
    this.registerSubmitButton = page.getByRole('dialog', { name: '注册账号' }).getByRole('button', { name: '注册' });
    this.registerCancelButton = page.getByRole('dialog', { name: '注册账号' }).getByRole('button', { name: '取消' });
  }

  async goto() {
    await this.navigateTo(ROUTES.login);
  }

  async login(username: string, password: string) {
    await this.usernameInput.fill(username);
    await this.passwordInput.fill(password);
    await this.loginButton.click();
  }

  async openRegisterDialog() {
    await this.registerLink.click();
    await this.registerDialog.waitFor({ state: 'visible' });
  }

  async register(username: string, password: string, nickname: string) {
    await this.openRegisterDialog();
    await this.registerUsernameInput.fill(username);
    await this.registerPasswordInput.fill(password);
    await this.registerConfirmPasswordInput.fill(password);
    await this.registerNicknameInput.fill(nickname);
    await this.registerSubmitButton.click();
  }

  async isOnLoginPage(): Promise<boolean> {
    return await this.page.locator('.login-container').isVisible();
  }
}
