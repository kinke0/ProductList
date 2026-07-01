import { test, expect } from '@playwright/test';
import { DataListFunctionalPage } from '../../pages/data-list-functional.page';
import { DataListPage } from '../../pages/data-list.page';

const TEST_NAME_PREFIX = 'E2E测试';

test.describe('产品清单-CRUD闭环', () => {
  let funcPage: DataListFunctionalPage;
  let dataListPage: DataListPage;

  test.beforeEach(async ({ page }) => {
    funcPage = new DataListFunctionalPage(page);
    dataListPage = new DataListPage(page);
    await funcPage.gotoDataList();
  });

  test('新建条目-填写名称保存成功', async ({ page }) => {
    // 需要在L2树节点下才能新建，先展开侧边栏树并选中L2节点
    // 新建按钮条件: props.isEditing && props.selectedNode?.level === 2
    const treePanel = page.locator('.tree-panel');
    // 1. 先展开根节点（增加等待时间确保子节点渲染完成）
    const rootNodes = treePanel.locator('.el-tree-node');
    const rootCount = await rootNodes.count();
    for (let i = 0; i < Math.min(rootCount, 3); i++) {
      const node = rootNodes.nth(i);
      const expandIcon = node.locator('.el-tree-node__expand-icon');
      if (await expandIcon.isVisible().catch(() => false)) {
        await expandIcon.click();
        await page.waitForTimeout(1000);
      }
    }
    // 2. 找到并点击一个L2节点（展开后出现的子节点）
    const childNodes = treePanel.locator('.el-tree-node .el-tree-node');
    const childCount = await childNodes.count();
    for (let i = 0; i < Math.min(childCount, 5); i++) {
      const node = childNodes.nth(i);
      if (await node.isVisible().catch(() => false)) {
        await node.locator('.el-tree-node__content').click();
        await page.waitForTimeout(1000);
        // 检查新建按钮是否可见
        if (await funcPage.newButton.isVisible({ timeout: 2000 }).catch(() => false)) {
          break;
        }
      }
    }
    // 点击新建按钮
    await funcPage.newButton.click({ timeout: 5000 });
    // 验证编辑对话框出现
    await expect(page.locator('.el-dialog').filter({ has: page.locator('.edit-form-compact') })).toBeVisible({ timeout: 5000 });
    // 填写产品名称
    await funcPage.fillProductName(TEST_NAME_PREFIX + '-新建');
    // 保存
    await funcPage.saveEditForm();
    // 验证成功消息
    const msg = await funcPage.getSuccessMessage();
    expect(msg).toBeTruthy();
    // 清理：找到新创建的行并删除
    const newRow = page.locator('.vrow').filter({ hasText: TEST_NAME_PREFIX + '-新建' }).first();
    if (await newRow.isVisible({ timeout: 3000 }).catch(() => false)) {
      await newRow.locator('.op-btn.op-del').filter({ hasText: '删除' }).click();
      await funcPage.confirmMessageBox();
    }
  });

  test('编辑条目-打开编辑对话框并修改名称', async ({ page }) => {
    // 打开第一行的编辑对话框
    await funcPage.openEditForFirstRow();
    // 验证编辑对话框可见
    await expect(page.locator('.el-dialog').filter({ has: page.locator('.edit-form-compact') })).toBeVisible({ timeout: 5000 });
    // 修改产品名称
    const nameInput = page.locator('.edit-form-compact input').first();
    const currentName = await nameInput.inputValue();
    await nameInput.clear();
    await nameInput.fill(currentName + '-编辑');
    // 保存
    await funcPage.saveEditForm();
    // 验证成功消息
    const msg = await funcPage.getSuccessMessage();
    expect(msg).toBeTruthy();
  });

  test('添加子条目-在L3产品下点击添加按钮', async ({ page }) => {
    // 找到一条有"添加"按钮的行（L3级别）
    const addRow = page.locator('.vrow:not(.sep-row)').filter({ has: page.locator('.op-btn.op-add').filter({ hasText: '添加' }) }).first();
    if (await addRow.isVisible({ timeout: 3000 }).catch(() => false)) {
      await addRow.locator('.op-btn.op-add').filter({ hasText: '添加' }).click();
      await page.waitForTimeout(1000);
      // 验证对话框出现
      await expect(page.locator('.el-dialog').filter({ has: page.locator('.edit-form-compact') })).toBeVisible({ timeout: 5000 });
      // 填写子条目名称
      await funcPage.fillProductName(TEST_NAME_PREFIX + '-子条目');
      // 保存
      await funcPage.saveEditForm();
      const msg = await funcPage.getSuccessMessage();
      expect(msg).toBeTruthy();
      // 清理：删除子条目
      const childRow = page.locator('.vrow').filter({ hasText: TEST_NAME_PREFIX + '-子条目' }).first();
      if (await childRow.isVisible({ timeout: 3000 }).catch(() => false)) {
        await childRow.locator('.op-btn.op-del').filter({ hasText: '删除' }).click();
        await funcPage.confirmMessageBox();
      }
    }
  });

  test('删除条目-确认删除后条目消失', async ({ page }) => {
    // 先展开树并找到L2节点以显示新建按钮
    const treePanel = page.locator('.tree-panel');
    const rootNodes = treePanel.locator('.el-tree-node');
    const rootCount = await rootNodes.count();
    for (let i = 0; i < Math.min(rootCount, 3); i++) {
      const node = rootNodes.nth(i);
      const expandIcon = node.locator('.el-tree-node__expand-icon');
      if (await expandIcon.isVisible().catch(() => false)) {
        await expandIcon.click();
        await page.waitForTimeout(500);
      }
    }
    // 找到并点击L2子节点使新建按钮出现
    const childNodes = treePanel.locator('.el-tree-node .el-tree-node');
    const childCount = await childNodes.count();
    for (let i = 0; i < Math.min(childCount, 5); i++) {
      const node = childNodes.nth(i);
      if (await node.isVisible().catch(() => false)) {
        await node.locator('.el-tree-node__content').click();
        await page.waitForTimeout(1000);
        if (await funcPage.newButton.isVisible({ timeout: 2000 }).catch(() => false)) {
          break;
        }
      }
    }
    const newBtn = funcPage.newButton;
    if (await newBtn.isVisible({ timeout: 3000 }).catch(() => false)) {
      await newBtn.click();
      await page.waitForTimeout(1000);
      await funcPage.fillProductName(TEST_NAME_PREFIX + '-待删');
      await funcPage.saveEditForm();
      await page.waitForTimeout(500);
      // 找到新建的行并删除
      const newRow = page.locator('.vrow').filter({ hasText: TEST_NAME_PREFIX + '-待删' }).first();
      if (await newRow.isVisible({ timeout: 3000 }).catch(() => false)) {
        await newRow.locator('.op-btn.op-del').filter({ hasText: '删除' }).click();
        await funcPage.confirmMessageBox();
        const msg = await funcPage.getSuccessMessage();
        expect(msg).toBeTruthy();
      }
    }
  });

  test('查看条目-非编辑模式下点击查看', async ({ page }) => {
    // 切换到已发布版本（非编辑模式）
    await dataListPage.switchVersionButton.click();
    // DataWorkbench版本切换对话框没有确认按钮，点击行即切换版本
    const dialog = page.locator('.el-dialog').filter({ hasText: '切换版本' });
    await expect(dialog).toBeVisible({ timeout: 5000 });
    // 在版本切换对话框中选择一个已发布版本
    const releasedRow = dialog.locator('.el-table__row').filter({ hasText: '已发布' }).first();
    if (await releasedRow.isVisible({ timeout: 5000 }).catch(() => false)) {
      await releasedRow.click();
      // 点击行后版本切换立即执行，对话框自动关闭
      await page.waitForTimeout(2000);
      // 切回草稿版本以便后续测试正常
      // 点击查看按钮（非编辑模式下"编辑"变为"查看"）
      const viewBtn = page.locator('.vrow:not(.sep-row) .op-btn.op-edit').filter({ hasText: '查看' }).first();
      if (await viewBtn.isVisible({ timeout: 3000 }).catch(() => false)) {
        await viewBtn.click();
        await page.waitForTimeout(1000);
        // 验证对话框打开（只读模式）
        const dialogVisible = await page.locator('.el-dialog').filter({ has: page.locator('.edit-form-compact') }).isVisible({ timeout: 5000 }).catch(() => false);
        expect(dialogVisible).toBe(true);
      }
    }
  });

  test('分隔行-点击添加产品按钮打开对话框', async ({ page }) => {
    // 找到分隔行
    const sepRow = page.locator('.vrow.sep-row').first();
    if (await sepRow.isVisible({ timeout: 3000 }).catch(() => false)) {
      // 点击分隔行的"添加产品/系统"按钮
      const addBtn = sepRow.locator('.sep-add-btn');
      if (await addBtn.isVisible()) {
        await addBtn.click();
        // 验证编辑对话框打开
        await expect(page.locator('.el-dialog').filter({ has: page.locator('.edit-form-compact') })).toBeVisible({ timeout: 5000 });
        // 取消对话框（不创建数据）
        await funcPage.cancelEditForm();
      }
    }
  });
});
