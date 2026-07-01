import { BasePage } from './base.page';
import { Locator, Page } from '@playwright/test';
import { ROUTES } from '../fixtures/test-data';

export class UserManagePage extends BasePage {
  readonly headerTitle: Locator;
  readonly addUserButton: Locator;
  readonly userTable: Locator;
  readonly dialog: Locator;
  readonly usernameInput: Locator;
  readonly nicknameInput: Locator;
  readonly roleSelect: Locator;
  readonly statusSwitch: Locator;
  readonly saveButton: Locator;
  readonly cancelButton: Locator;

  constructor(page: Page) {
    super(page);
    this.headerTitle = page.getByText('用户管理');
    this.addUserButton = page.getByRole('button', { name: '新增用户' });
    this.userTable = page.locator('.page .el-table');
    this.dialog = page.getByRole('dialog');
    this.usernameInput = this.dialog.getByText('用户名').locator('..').locator('input');
    this.nicknameInput = this.dialog.getByText('姓名').locator('..').locator('input');
    this.roleSelect = this.dialog.locator('.el-select');
    this.statusSwitch = this.dialog.getByRole('switch');
    this.saveButton = this.dialog.getByRole('button', { name: '保存' });
    this.cancelButton = this.dialog.getByRole('button', { name: '取消' });
  }

  async goto() {
    await this.navigateTo(ROUTES.userManage);
  }

  async getRowCount(): Promise<number> {
    return await this.userTable.getByRole('row').count();
  }

  async clickEditForRow(rowIndex: number) {
    await this.userTable.getByRole('row').nth(rowIndex).getByRole('button', { name: '编辑' }).click();
  }

  async clickLogForRow(rowIndex: number) {
    await this.userTable.getByRole('row').nth(rowIndex).getByRole('button', { name: '操作日志' }).click();
  }

  async clickDeleteForRow(rowIndex: number) {
    await this.userTable.getByRole('row').nth(rowIndex).getByRole('button', { name: '删除' }).click();
  }

  async openAddDialog() {
    await this.addUserButton.click();
    await this.dialog.waitFor({ state: 'visible' });
  }

  async fillUserForm(username: string, nickname: string) {
    await this.usernameInput.fill(username);
    await this.nicknameInput.fill(nickname);
  }

  async saveUser() {
    await this.saveButton.click();
  }
}
