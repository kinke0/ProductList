import { BasePage } from './base.page';
import { Locator, Page, expect } from '@playwright/test';
import { ROUTES } from '../fixtures/test-data';

export class DataListFunctionalPage extends BasePage {
  // 工具栏按钮
  readonly newButton: Locator;
  readonly insertButton: Locator;
  readonly batchDropdown: Locator;
  readonly renumberButton: Locator;

  // 虚拟表格
  readonly virtualTable: Locator;
  readonly firstNonSepRow: Locator;

  // 编辑对话框
  readonly editDialog: Locator;
  readonly productNameInput: Locator;
  readonly intelligentCheckbox: Locator;
  readonly saveButton: Locator;
  readonly dialogCancelButton: Locator;

  // 右键菜单
  readonly contextMenu: Locator;

  // 批量对话框
  readonly batchStatusDialog: Locator;
  readonly batchSolutionDialog: Locator;
  readonly batchManagerDialog: Locator;
  readonly batchCategoryDialog: Locator;
  readonly batchVersionDialog: Locator;

  constructor(page: Page) {
    super(page);
    this.newButton = page.getByRole('button', { name: '新建' });
    this.insertButton = page.getByRole('button', { name: '插入待生成清单' });
    this.batchDropdown = page.getByRole('button', { name: '其他批量操作' });
    this.renumberButton = page.getByRole('button', { name: '编码重排序' });

    this.virtualTable = page.locator('.virtual-table');
    this.firstNonSepRow = page.locator('.vrow:not(.sep-row)').first();

    this.editDialog = page.locator('.el-dialog').filter({ has: page.locator('.edit-form-compact') });
    this.productNameInput = page.locator('.edit-form-compact input').first();
    this.intelligentCheckbox = page.locator('.intelligent-box .el-checkbox');
    this.saveButton = page.getByRole('button', { name: '保存' });
    this.dialogCancelButton = page.getByRole('button', { name: '取消' });

    this.contextMenu = page.locator('.ctx-menu');

    this.batchStatusDialog = page.locator('.el-dialog').filter({ hasText: '批量修改功能状态' });
    this.batchSolutionDialog = page.locator('.el-dialog').filter({ hasText: '批量修改解决方案' });
    this.batchManagerDialog = page.locator('.el-dialog').filter({ hasText: '批量指定产品经理' });
    this.batchCategoryDialog = page.locator('.el-dialog').filter({ hasText: '批量修改业务分类' });
    this.batchVersionDialog = page.locator('.el-dialog').filter({ hasText: '批量修改版本划分' });
  }

  rowById(id: number): Locator {
    return this.page.locator(`.row-id-${id}`);
  }

  async gotoDataList(): Promise<void> {
    await this.navigateTo(ROUTES.dashboard);
    // 选择草稿版本（status === 'draft'），编辑操作只在草稿版本下可用
    // UI显示"编辑中"标签对应draft状态
    const draftRow = this.page.locator('.version-pick .el-table__body .el-table__row').filter({ hasText: /编辑中|草稿|draft/i }).first();
    if (await draftRow.isVisible()) {
      await draftRow.click();
      await this.page.waitForLoadState('networkidle');
    } else {
      // 没有草稿版本时选择第一个版本
      const firstRow = this.page.locator('.version-pick .el-table__body .el-table__row').first();
      if (await firstRow.isVisible()) {
        await firstRow.click();
        await this.page.waitForLoadState('networkidle');
      }
    }
    // 切换到数据清单tab
    const listTab = this.page.locator('.el-tabs__item').filter({ hasText: '数据清单' });
    if (await listTab.isVisible()) {
      await listTab.click();
      await this.page.waitForLoadState('networkidle');
    }
  }

  async rightClickRow(rowId: number): Promise<void> {
    const row = this.rowById(rowId);
    if (!(await row.isVisible())) await this.scrollToRow(rowId);
    await row.click({ button: 'right' });
  }

  async rightClickFirstNonSepRow(): Promise<void> {
    await this.firstNonSepRow.click({ button: 'right' });
  }

  async clickContextMenuItem(name: string): Promise<void> {
    await this.contextMenu.locator('.ctx-menu-item').filter({ hasText: name }).click();
  }

  async selectRowCheckbox(rowId: number): Promise<void> {
    const row = this.rowById(rowId);
    if (!(await row.isVisible())) await this.scrollToRow(rowId);
    await row.locator('.el-checkbox').first().click();
  }

  async selectFirstNonSepRowCheckbox(): Promise<void> {
    await this.firstNonSepRow.locator('.el-checkbox').first().click();
  }

  async scrollToRow(rowId: number): Promise<void> {
    const row = this.rowById(rowId);
    for (let i = 0; i < 50; i++) {
      if (await row.isVisible()) return;
      await this.page.mouse.wheel(0, 300);
      await this.page.waitForTimeout(100);
    }
  }

  async openEditForRow(rowId: number): Promise<void> {
    const row = this.rowById(rowId);
    if (!(await row.isVisible())) await this.scrollToRow(rowId);
    await row.locator('.op-btn.op-edit').filter({ hasText: '编辑' }).click();
    await this.page.waitForTimeout(1000);
  }

  async openEditForFirstRow(): Promise<void> {
    await this.firstNonSepRow.locator('.op-btn.op-edit').filter({ hasText: '编辑' }).click();
    await this.page.waitForTimeout(1000);
  }

  async addChildForRow(rowId: number): Promise<void> {
    const row = this.rowById(rowId);
    if (!(await row.isVisible())) await this.scrollToRow(rowId);
    await row.locator('.op-btn.op-add').filter({ hasText: '添加' }).click();
    await this.page.waitForTimeout(1000);
  }

  async deleteRowViaButton(rowId: number): Promise<void> {
    const row = this.rowById(rowId);
    if (!(await row.isVisible())) await this.scrollToRow(rowId);
    await row.locator('.op-btn.op-del').filter({ hasText: '删除' }).click();
    // 确认删除对话框 - 使用 CSS类选择确认按钮
    const confirmBtn = this.page.locator('.el-message-box__btns .el-button--primary');
    await confirmBtn.click({ timeout: 5000 });
    await this.page.waitForTimeout(1000);
  }

  async deleteRowViaButtonCancel(rowId: number): Promise<void> {
    const row = this.rowById(rowId);
    if (!(await row.isVisible())) await this.scrollToRow(rowId);
    await row.locator('.op-btn.op-del').filter({ hasText: '删除' }).click();
    const cancelBtn = this.page.locator('.el-message-box__btns .el-button:not(.el-button--primary)');
    await cancelBtn.click({ timeout: 5000 });
  }

  async fillProductName(name: string): Promise<void> {
    await this.productNameInput.clear();
    await this.productNameInput.fill(name);
    await this.page.waitForTimeout(500);
  }

  async saveEditForm(): Promise<void> {
    await this.saveButton.click();
    await this.page.waitForTimeout(2000);
  }

  async cancelEditForm(): Promise<void> {
    await this.dialogCancelButton.click();
  }

  async openBatchDropdownCommand(command: string): Promise<void> {
    await this.batchDropdown.click({ timeout: 5000 });
    const menuitem = this.page.getByRole('menuitem', { name: command });
    await menuitem.click({ timeout: 5000 });
    await this.page.waitForTimeout(500);
  }

  async confirmMessageBox(): Promise<void> {
    // Element Plus MessageBox 确认按钮使用 .el-button--primary CSS类
    // 不依赖 getByRole(name) 因内部结构可能导致 accessible name 不匹配
    const confirmBtn = this.page.locator('.el-message-box__btns .el-button--primary');
    await confirmBtn.click({ timeout: 5000 });
  }

  async cancelMessageBox(): Promise<void> {
    const cancelBtn = this.page.locator('.el-message-box__btns .el-button:not(.el-button--primary)');
    await cancelBtn.click({ timeout: 5000 });
  }

  async getSuccessMessage(): Promise<string | null> {
    const msg = this.page.locator('.el-message--success');
    try {
      await msg.waitFor({ state: 'visible', timeout: 5000 });
      const text = await msg.textContent();
      return text;
    } catch {
      return null;
    }
  }

  async waitForDataLoaded(): Promise<void> {
    // 等待数据加载遮罩消失
    const overlay = this.page.locator('.batch-overlay');
    if (await overlay.isVisible()) {
      await overlay.waitFor({ state: 'hidden', timeout: 15000 });
    }
  }

  async getFirstRowId(): Promise<number | null> {
    const row = this.firstNonSepRow;
    const className = await row.getAttribute('class') || '';
    const match = className.match(/row-id-(\d+)/);
    return match ? parseInt(match[1]) : null;
  }

  async getFirstRowLevel(): Promise<number | null> {
    const row = this.firstNonSepRow;
    const className = await row.getAttribute('class') || '';
    const match = className.match(/row-level-(\d+)/);
    return match ? parseInt(match[1]) : null;
  }

  async getRowCount(): Promise<number> {
    return await this.page.locator('.vrow:not(.sep-row)').count();
  }
}
