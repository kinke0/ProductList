import { BasePage } from './base.page';
import { Locator, Page } from '@playwright/test';
import { ROUTES } from '../fixtures/test-data';

export class RequirementPage extends BasePage {
  readonly headerTitle: Locator;
  readonly createButton: Locator;
  readonly scopeMyRadio: Locator;
  readonly scopeAllRadio: Locator;
  readonly statusSelect: Locator;
  readonly categorySelect: Locator;
  readonly typeSelect: Locator;
  readonly prioritySelect: Locator;
  readonly creatorInput: Locator;
  readonly searchButton: Locator;
  readonly resetButton: Locator;
  readonly requirementTable: Locator;

  constructor(page: Page) {
    super(page);
    this.headerTitle = page.getByText('需求管理');
    this.createButton = page.getByRole('button', { name: '提交需求' });
    this.scopeMyRadio = page.getByRole('radio', { name: '我的需求' });
    this.scopeAllRadio = page.getByRole('radio', { name: '全部需求' });
    this.statusSelect = page.locator('.filter-bar').getByPlaceholder('状态');
    this.categorySelect = page.locator('.filter-bar').getByPlaceholder('所属模块');
    this.typeSelect = page.locator('.filter-bar').getByPlaceholder('需求类型');
    this.prioritySelect = page.locator('.filter-bar').getByPlaceholder('优先级');
    this.creatorInput = page.locator('.filter-bar').getByPlaceholder('提出人');
    this.searchButton = page.locator('.filter-bar').getByRole('button', { name: '搜索' });
    this.resetButton = page.locator('.filter-bar').getByRole('button', { name: '重置' });
    this.requirementTable = page.locator('.table-panel .el-table');
  }

  async goto() {
    await this.navigateTo(ROUTES.requirement);
  }

  async switchToAllRequirements() {
    await this.scopeAllRadio.click({ force: true });
    await this.waitForLoadingToFinish();
  }

  async switchToMyRequirements() {
    await this.scopeMyRadio.click({ force: true });
    await this.waitForLoadingToFinish();
  }
}
