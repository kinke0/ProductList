import { BasePage } from './base.page';
import { Locator, Page } from '@playwright/test';
import { ROUTES } from '../fixtures/test-data';

export class BaseDataPage extends BasePage {
  readonly headerTitle: Locator;
  readonly addButton: Locator;
  readonly dataTable: Locator;

  constructor(page: Page) {
    super(page);
    this.headerTitle = page.locator('.page h3');
    this.addButton = page.getByRole('button', { name: /新增|添加/ });
    this.dataTable = page.locator('.page .el-table');
  }

  async gotoCategory() {
    await this.navigateTo(ROUTES.baseDataCategory);
  }

  async gotoSolution() {
    await this.navigateTo(ROUTES.baseDataSolution);
  }

  async gotoStatus() {
    await this.navigateTo(ROUTES.baseDataStatus);
  }

  async gotoSystemType() {
    await this.navigateTo(ROUTES.baseDataSystemType);
  }
}
