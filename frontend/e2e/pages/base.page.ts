import { Locator, Page } from '@playwright/test';

export class BasePage {
  readonly page: Page;

  constructor(page: Page) {
    this.page = page;
  }

  async navigateTo(path: string) {
    await this.page.goto(path);
    await this.page.waitForLoadState('networkidle');
  }

  async waitForApi(endpoint: string, status = 200) {
    return this.page.waitForResponse(
      (resp) => resp.url().includes(endpoint) && resp.status() === status
    );
  }

  async getSuccessMessage(): Promise<string> {
    const msg = this.page.locator('.el-message--success');
    await msg.waitFor({ state: 'visible', timeout: 5000 });
    const text = await msg.textContent();
    return text || '';
  }

  async getErrorMessage(): Promise<string> {
    const msg = this.page.locator('.el-message--error');
    await msg.waitFor({ state: 'visible', timeout: 5000 });
    const text = await msg.textContent();
    return text || '';
  }

  async confirmDialog(actionName = '确认') {
    const dialog = this.page.getByRole('dialog');
    await dialog.getByRole('button', { name: actionName }).click();
  }

  async cancelDialog() {
    const dialog = this.page.getByRole('dialog');
    await dialog.getByRole('button', { name: '取消' }).click();
  }

  async selectDropdownOption(selector: Locator, optionText: string) {
    await selector.click();
    const option = this.page.getByRole('listitem').filter({ hasText: optionText }).first();
    await option.click();
  }

  async clickMenu(menuText: string) {
    await this.page.getByRole('menuitem', { name: menuText }).click();
  }

  async clickTab(tabText: string) {
    await this.page.getByRole('tab', { name: tabText }).click();
  }

  async waitForLoadingToFinish() {
    const loading = this.page.locator('.el-loading-mask');
    if (await loading.isVisible()) {
      await loading.waitFor({ state: 'hidden', timeout: 15000 });
    }
  }
}
