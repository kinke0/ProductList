import { BasePage } from './base.page';
import { Locator, Page } from '@playwright/test';
import { ROUTES } from '../fixtures/test-data';

export class VersionManagePage extends BasePage {
  readonly headerTitle: Locator;
  readonly createVersionButton: Locator;
  readonly versionTable: Locator;
  readonly statusBar: Locator;
  readonly progressDialog: Locator;

  constructor(page: Page) {
    super(page);
    this.headerTitle = page.getByText('版本管理');
    this.createVersionButton = page.getByRole('button', { name: '创建新版本' });
    this.versionTable = page.locator('.page .el-table');
    this.statusBar = page.locator('.status-bar');
    this.progressDialog = page.getByRole('dialog');
  }

  async goto() {
    await this.navigateTo(ROUTES.versionManage);
  }

  async getVersionCount(): Promise<number> {
    return await this.versionTable.getByRole('row').count();
  }
}
