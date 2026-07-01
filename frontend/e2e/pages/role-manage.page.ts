import { BasePage } from './base.page';
import { Locator, Page } from '@playwright/test';
import { ROUTES } from '../fixtures/test-data';

export class RoleManagePage extends BasePage {
  readonly headerTitle: Locator;
  readonly addButton: Locator;
  readonly roleTable: Locator;
  readonly dialog: Locator;
  readonly nameInput: Locator;
  readonly codeInput: Locator;
  readonly descriptionInput: Locator;
  readonly saveButton: Locator;
  readonly cancelButton: Locator;

  constructor(page: Page) {
    super(page);
    this.headerTitle = page.getByText('权限套餐管理');
    this.addButton = page.getByRole('button', { name: '新增套餐' });
    this.roleTable = page.locator('.page .el-table');
    this.dialog = page.getByRole('dialog', { name: '新增权限套餐' });
    this.nameInput = this.dialog.locator('.el-form-item').filter({ hasText: '名称' }).locator('input');
    this.codeInput = this.dialog.locator('.el-form-item').filter({ hasText: '编码' }).locator('input');
    this.descriptionInput = this.dialog.locator('.el-form-item').filter({ hasText: '描述' }).locator('textarea');
    this.saveButton = this.dialog.getByRole('button', { name: '保存' });
    this.cancelButton = this.dialog.getByRole('button', { name: '取消' });
  }

  async goto() {
    await this.navigateTo(ROUTES.roleManage);
  }
}
