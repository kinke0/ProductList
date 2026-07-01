import { test, expect } from '@playwright/test';
import { DataListFunctionalPage } from '../../pages/data-list-functional.page';

test.describe('产品清单-批量操作', () => {
  let funcPage: DataListFunctionalPage;

  test.beforeEach(async ({ page }) => {
    funcPage = new DataListFunctionalPage(page);
    await funcPage.gotoDataList();
  });

  test('批量修改状态-勾选后打开对话框', async ({ page }) => {
    // 勾选两条行
    await funcPage.selectFirstNonSepRowCheckbox();
    // 点击"其他批量操作" → "状态修改"
    await funcPage.openBatchDropdownCommand('状态修改');
    // 验证批量状态对话框出现
    await expect(funcPage.batchStatusDialog).toBeVisible({ timeout: 5000 });
  });

  test('批量修改解决方案-勾选后打开对话框', async ({ page }) => {
    await funcPage.selectFirstNonSepRowCheckbox();
    await funcPage.openBatchDropdownCommand('解决方案');
    await expect(funcPage.batchSolutionDialog).toBeVisible({ timeout: 5000 });
  });

  test('批量指定产品经理-勾选后打开对话框', async ({ page }) => {
    await funcPage.selectFirstNonSepRowCheckbox();
    await funcPage.openBatchDropdownCommand('指定产品经理');
    await expect(funcPage.batchManagerDialog).toBeVisible({ timeout: 5000 });
  });

  test('批量修改业务分类域-勾选后打开对话框', async ({ page }) => {
    await funcPage.selectFirstNonSepRowCheckbox();
    await funcPage.openBatchDropdownCommand('修改业务分类/业务域');
    await expect(funcPage.batchCategoryDialog).toBeVisible({ timeout: 5000 });
  });

  test('批量版本划分-勾选后打开对话框', async ({ page }) => {
    await funcPage.selectFirstNonSepRowCheckbox();
    await funcPage.openBatchDropdownCommand('版本划分');
    await expect(funcPage.batchVersionDialog).toBeVisible({ timeout: 5000 });
  });

  test('批量删除-确认对话框出现后取消', async ({ page }) => {
    await funcPage.selectFirstNonSepRowCheckbox();
    // 点击批量删除
    await funcPage.openBatchDropdownCommand('批量删除');
    // 验证确认删除对话框出现
    await expect(page.locator('.el-message-box')).toBeVisible({ timeout: 5000 });
    // 取消删除（不执行真实删除）
    await funcPage.cancelMessageBox();
  });
});
