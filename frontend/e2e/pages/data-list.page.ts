import { BasePage } from './base.page';
import { Locator, Page } from '@playwright/test';
import { ROUTES } from '../fixtures/test-data';

export class DataListPage extends BasePage {
  // 版本选择
  readonly versionPickSection: Locator;
  readonly versionTable: Locator;

  // 工作台
  readonly workbenchHeader: Locator;
  readonly versionBadge: Locator;
  readonly switchVersionButton: Locator;

  // 标签页
  readonly panoramaTab: Locator;
  readonly statsTab: Locator;
  readonly listTab: Locator;
  readonly addListButton: Locator;

  // 搜索栏
  readonly searchNameInput: Locator;
  readonly searchStatusSelect: Locator;
  readonly searchProductManagerInput: Locator;
  readonly searchSolutionSelect: Locator;
  readonly searchVersionDivSelect: Locator;
  readonly intelligentCheckbox: Locator;
  readonly searchButton: Locator;
  readonly resetButton: Locator;

  // 工具栏
  readonly expandAllButton: Locator;
  readonly collapseAllButton: Locator;
  readonly newButton: Locator;

  // 侧边栏
  readonly sidebarToggle: Locator;

  constructor(page: Page) {
    super(page);
    // 版本选择区
    this.versionPickSection = page.locator('.version-pick');
    this.versionTable = page.locator('.version-pick .el-table');

    // 工作台头部
    this.workbenchHeader = page.locator('.workbench-header');
    this.versionBadge = page.locator('.version-badge');
    this.switchVersionButton = page.getByRole('button', { name: '切换版本' });

    // 标签页
    this.panoramaTab = page.getByRole('tab', { name: '产品全景图' });
    this.statsTab = page.getByRole('tab', { name: '统计视图' });
    this.listTab = page.getByRole('tab', { name: '数据清单' });
    this.addListButton = page.locator('.add-list-tab-btn');

    // 搜索栏
    this.searchNameInput = page.getByPlaceholder('产品/系统名称');
    this.searchStatusSelect = page.locator('.query-bar').getByText('状态').locator('..').locator('.el-select');
    this.searchProductManagerInput = page.getByPlaceholder('产品经理');
    this.searchSolutionSelect = page.locator('.query-bar').getByText('解决方案').locator('..').locator('.el-select');
    this.searchVersionDivSelect = page.locator('.query-bar').getByText('版本').locator('..').locator('.el-select');
    this.intelligentCheckbox = page.getByRole('checkbox', { name: '智能化' });
    this.searchButton = page.getByRole('button', { name: '查询' });
    this.resetButton = page.getByRole('button', { name: '重置' });

    // 工具栏
    this.expandAllButton = page.getByText('全部展开');
    this.collapseAllButton = page.getByText('全部折叠');
    this.newButton = page.getByRole('button', { name: '新建' });

    // 侧边栏
    this.sidebarToggle = page.locator('.sidebar-toggle');
  }

  async goto() {
    await this.navigateTo(ROUTES.dataList);
  }

  async selectVersion(index = 0) {
    // 在版本选择表中选择第一个版本
    if (await this.versionPickSection.isVisible()) {
      const rows = this.versionTable.getByRole('row');
      const count = await rows.count();
      if (count > index) {
        await rows.nth(index).click();
        await this.page.waitForLoadState('networkidle');
      }
    }
  }

  async switchToListTab() {
    await this.listTab.click();
    await this.waitForLoadingToFinish();
  }

  async search(keyword: string) {
    await this.searchNameInput.fill(keyword);
    await this.searchButton.click();
    await this.waitForLoadingToFinish();
  }

  async resetSearch() {
    await this.resetButton.click();
    await this.waitForLoadingToFinish();
  }

  async expandAll() {
    await this.expandAllButton.click();
  }

  async collapseAll() {
    await this.collapseAllButton.click();
  }

  async getListItemCount(): Promise<number> {
    // 获取虚拟滚动列表中可见的行数
    return await this.page.locator('.data-list-tab .el-table__body-wrapper .el-table__row').count();
  }

  async clickTreeNode(nodeName: string) {
    const tree = this.page.locator('.sidebar-content');
    await tree.getByText(nodeName).click();
    await this.waitForLoadingToFinish();
  }

  async getQueryResultText(): Promise<string> {
    const title = this.page.locator('.toolbar-title');
    return await title.textContent() || '';
  }
}
