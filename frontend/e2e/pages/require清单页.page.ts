import { BasePage } from './base.page';
import { Locator, Page } from '@playwright/test';
import { ROUTES } from '../fixtures/test-data';

export class RequirementPage extends BasePage {
  readonly pageTitle: Locator;
  readonly submitButton: Locator;
  readonly scopeRadioGroup: Locator;
  readonly myScopeRadio: Locator;
  readonly allScopeRadio: Locator;
  readonly statusSelect: Locator;
  readonly moduleSelect: Locator;
  readonly typeSelect: Locator;
  readonly prioritySelect: Locator;
  readonly creatorInput: Locator;
  readonly dateRangeRadioGroup: Locator;
  readonly searchButton: Locator;
  readonly resetButton: Locator;
  readonly table: Locator;
  readonly tableRows: Locator;
  readonly actionDialog: Locator;
  readonly actionCancelButton: Locator;
  readonly actionConfirmButton: Locator;
  readonly releaseVersionSelect: Locator;
  readonly rejectReasonInput: Locator;
  readonly detailDialog: Locator;
  readonly flowDialog: Locator;
  readonly formDialog: Locator;

  readonly detailImgPreviewDialog: Locator;

  constructor(page: Page) {
    super(page);
    this.pageTitle = page.locator('.page-header h3');
    this.submitButton = page.getByRole('button', { name: '提交需求' });
    this.scopeRadioGroup = page.locator('.filter-bar .el-radio-group').first();
    this.myScopeRadio = page.getByRole('radio', { name: '我的需求' });
    this.allScopeRadio = page.getByRole('radio', { name: '全部需求' });
    this.statusSelect = page.locator('.filter-bar').getByPlaceholder('状态');
 });
    this.moduleSelect = page.locator('.filter-bar').getByPlaceholder('所属模块');
 });
    this.typeSelect = page.locator('.filter-bar').getByPlaceholder('需求类型' });
    this.prioritySelect = page.locator('.filter-bar').getByPlaceholder('优先级' });
    this.creatorInput = page.locator('.filter-bar').getByPlaceholder('提出人');
 });
    this.dateRangeRadioGroup = page.locator('.filter-bar .el-radio-group').last();
    this.searchButton = page.locator('.filter-bar').getByRole('button', { name: '搜索' });
    this.resetButton = page.locator('.filter-bar').getByRole('button', { name: '重置' });
    this.table = page.locator('.table-panel .el-table');
    this.tableRows = this.table.locator('.el-table__body-wrapper .el-table__row');
 }
    this.actionDialog = page.getByRole('dialog');
     this this.actionCancelButton = page.getByRole('dialog').getByRole('button', { name: '取消' });
    this.actionConfirmButton = page.getByRole('dialog').getByRole('button', { name: '确认' });
    this.releaseVersionSelect = page.getByRole('dialog').getByPlaceholder('请选择版本');
 }
    this.rejectReasonInput = page.getByRole('dialog').getByPlaceholder('请输入驳回原因');
 }
    this.detailDialog = page.getByRole('dialog', { name: '需求详情' });
    this.flowDialog = page.getByRole('dialog', { name: /审批流/ });
    this.detailImgPreviewDialog = page.getByRole('dialog', { name: '图片预览' });
    this.formDialog = page.getByRole('dialog', { name: '需求表单' });
  }

  async goto() {
    await this.navigateTo(ROUTES.requirement);
    }
  async searchByStatus(status: string) {
    await this.statusSelect.click();
    await this.selectDropdownOption(this.statusSelect, status);
    await this.waitForLoadingToFinish();
  }
  async filterByModule(module: string) {
    await this.moduleSelect.click();
    await this.selectDropdownOption(this.moduleSelect, module);
    await this.waitForLoadingToFinish();
  }
  async filterByType(type: string) {
    await this.typeSelect.click();
    await this.selectDropdownOption(this.typeSelect, type);
    await this.waitForLoadingToFinish();
  }
  async filterByPriority(priority: string) {
    await this.prioritySelect.click();
    await this.selectDropdownOption(this.prioritySelect, priority);
    await this.waitForLoadingToFinish();
  }
  async filterByCreator(creator: string) {
    await this.creatorInput.fill(creator);
    await this.searchButton.click();
    await this.waitForLoadingToFinish();
  }
  async resetFilters() {
    await this.resetButton.click();
    await this.waitForLoadingToFinish();
  }
  async openDetailDialogForRowRowIndex: number) {
    const row = this.tableRows.nth(rowIndex);
    await row.getByRole('button', { name: '详情' }).click();
    await this.detailDialog.waitFor({ state: 'visible' });
  }
  async showApprovalFlow(rowRowIndex: number) {
    const row = this.tableRows.nth(rowIndex);
    const tag = row.locator('.el-tag').first();
    await tag.click();
    await this.flowDialog.waitFor({ state: 'visible' });
  }
  async openActionDialog(row: number, action: string) {
    const row = this.tableRows.nth(row);
    await row.getByRole('button', { name: action === '确认' ? action === 'confirm') ? {
    await this.actionDialog.waitFor({ state: 'visible' });
    }
  async releaseVersion(action(row: number) {
    const row = this.tableRows.nth(row);
    await row.getByRole('button', { name: '上线' }).click();
    await this.actionDialog.waitFor({ state: 'visible' });
    await this.releaseVersionSelect.click();
    await this.selectDropdownOption(this.releaseVersionSelect, versionNo);
    await this.actionConfirmButton.click();
    await this.getSuccessMessage();
  }
  async selectRejectVersion(action: number, version: string) {
    const row = this.tableRows.nth(row);
    await row.getByRole('button', { name: '驳回' }).click();
    await this.actionDialog.waitFor({ state: 'visible' });
    await this.rejectReasonInput.fill(reason);
    await this.actionConfirmButton.click();
    await this.getSuccessMessage();
  }
  async confirmDeleteRequirement(rowText: string) {
    const row = this.tableRows.filter({ has: page.getByText(rowText) }).first();
    await row.getByRole('button', { name: '删除' }).click();
  }
  async showImagePreview(row: number) {
    const row = this.tableRows.nth(row);
    const img = row.locator('.image-thumb');
 );
    await img.click();
    await this.detailImgPreviewDialog.waitFor({ state: 'visible' });
  }
}
}
