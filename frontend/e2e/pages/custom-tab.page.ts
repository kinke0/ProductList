import { BasePage } from './base.page';
import { Locator, Page } from '@playwright/test';
import { ROUTES } from '../fixtures/test-data';

export class CustomTabPage extends BasePage {
  // 工作台头部
  readonly workbenchHeader: Locator;
  readonly versionBadge: Locator;

  // 标签页
  readonly panoramaTab: Locator;
  readonly statsTab: Locator;
  readonly listTab: Locator;
  readonly addListButton: Locator;

  // 自定义清单标签页
  readonly customTabPane: Locator;

  // 自定义清单右键菜单
  readonly renameMenuItem: Locator;
  readonly deleteMenuItem: Locator;

  // 重命名对话框
  readonly renameDialog: Locator;
  readonly renameInput: Locator;
  readonly renameConfirmButton: Locator;

  // 删除确认对话框
  readonly deleteConfirmDialog: Locator;

  // 新增清单对话框
  readonly addListDialog: Locator;
  readonly addListNameInput: Locator;
  readonly addListConfirmButton: Locator;
  readonly addListCancelButton: Locator;

  // 筛选创建清单对话框
  readonly filterCreateDialog: Locator;
  readonly filterNameInput: Locator;
  readonly filterEntryNameInput: Locator;
  readonly filterConfirmButton: Locator;
  readonly filterCancelButton: Locator;

  // 数据清单
  readonly dataListTab: Locator;
  readonly table: Locator;

  constructor(page: Page) {
    super(page);

    // 工作台头部
    this.workbenchHeader = page.locator('.workbench-header');
    this.versionBadge = page.locator('.version-badge');

    // 标签页
    this.panoramaTab = page.getByRole('tab', { name: '产品全景图' });
    this.statsTab = page.getByRole('tab', { name: '统计视图' });
    this.listTab = page.getByRole('tab', { name: '数据清单' });
    this.addListButton = page.locator('.add-list-tab-btn');

    // 自定义清单标签页
    this.customTabPane = page.locator('.el-tabs__item').filter({ hasText: /^((?!产品全景|统计视图|数据清单|__add).)*$/ });

    // 重命名对话框
    this.renameDialog = page.getByRole('dialog', { name: '重命名' });
    this.renameInput = this.renameDialog.locator('input');
    this.renameConfirmButton = this.renameDialog.getByRole('button', { name: '确定' });

    // 新增清单对话框
    this.addListDialog = page.getByRole('dialog', { name: '新增清单' });
    this.addListNameInput = this.addListDialog.getByPlaceholder('请输入清单名称');
    this.addListConfirmButton = this.addListDialog.getByRole('button', { name: '确定' });
    this.addListCancelButton = this.addListDialog.getByRole('button', { name: '取消' });

    // 筛选创建清单对话框
    this.filterCreateDialog = page.getByRole('dialog', { name: '按筛选条件创建' });
    this.filterNameInput = this.filterCreateDialog.getByPlaceholder('清单名称');
    this.filterEntryNameInput = this.filterCreateDialog.getByPlaceholder('产品/系统名称');
    this.filterConfirmButton = this.filterCreateDialog.getByRole('button', { name: '创建' });
    this.filterCancelButton = this.filterCreateDialog.getByRole('button', { name: '取消' });

    // 数据清单
    this.dataListTab = page.locator('.data-list-tab');
    this.table = page.locator('.data-list-tab .el-table');
  }

  async goto() {
    await this.navigateTo(ROUTES.customTab);
    // 选择版本
    const versionRow = this.page.locator('.version-pick .el-table__body .el-table__row').first();
    if (await versionRow.isVisible()) {
      await versionRow.click();
      await this.page.waitForLoadState('networkidle');
    }
  }

  async clickAddListTabButton() {
    await this.addListButton.click();
  }

  async createCustomList(name: string) {
    await this.clickAddListTabButton();
    await this.addListDialog.waitFor({ state: 'visible' });
    await this.addListNameInput.fill(name);
    await this.addListConfirmButton.click();
  }

  async switchToCustomTab(tabName: string) {
    await this.page.getByRole('tab', { name: tabName }).click();
    await this.waitForLoadingToFinish();
  }

  async switchToListTab() {
    await this.listTab.click();
    await this.waitForLoadingToFinish();
  }

  async getCustomTabCount(): Promise<number> {
    // 自定义标签页不含全景图、统计视图、数据清单和添加按钮
    const allTabs = await this.page.locator('.el-tabs__item').count();
    return allTabs - 3; // 减去固定标签页数量（全景图、统计、清单）
  }
}
