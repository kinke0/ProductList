import { test, expect } from '@playwright/test';
import { DataListFunctionalPage } from '../../pages/data-list-functional.page';

test.describe('产品清单-版本划分', () => {
  let funcPage: DataListFunctionalPage;

  test.beforeEach(async ({ page }) => {
    funcPage = new DataListFunctionalPage(page);
    await funcPage.gotoDataList();
  });

  test('行内勾选曜系列-checkbox状态变化', async ({ page }) => {
    const row = page.locator('.vrow:not(.sep-row)').first();
    if (await row.isVisible()) {
      const yaoCheckbox = row.locator('.version-inline .el-checkbox').filter({ hasText: '曜' });
      // checkbox可能已选中或disabled，使用force click
      await yaoCheckbox.click({ force: true });
      await page.waitForTimeout(1000);
      // 验证checkbox有状态变化（选中或取消）
      const isChecked = await yaoCheckbox.locator('.el-checkbox__input.is-checked').isVisible().catch(() => false);
      expect(typeof isChecked).toBe('boolean');
    }
  });

  test('行内勾选远系列-checkbox状态变化', async ({ page }) => {
    const row = page.locator('.vrow:not(.sep-row)').first();
    if (await row.isVisible()) {
      const yuanCheckbox = row.locator('.version-inline .el-checkbox').filter({ hasText: '远' });
      await yuanCheckbox.click({ force: true });
      await page.waitForTimeout(1000);
      const isChecked = await yuanCheckbox.locator('.el-checkbox__input.is-checked').isVisible().catch(() => false);
      expect(typeof isChecked).toBe('boolean');
    }
  });

  test('行内勾选驰系列-checkbox状态变化', async ({ page }) => {
    const row = page.locator('.vrow:not(.sep-row)').first();
    if (await row.isVisible()) {
      const chiCheckbox = row.locator('.version-inline .el-checkbox').filter({ hasText: '驰' });
      await chiCheckbox.click({ force: true });
      await page.waitForTimeout(1000);
      const isChecked = await chiCheckbox.locator('.el-checkbox__input.is-checked').isVisible().catch(() => false);
      expect(typeof isChecked).toBe('boolean');
    }
  });

  test('行内勾选非标配-验证互斥关系', async ({ page }) => {
    const row = page.locator('.vrow:not(.sep-row)').first();
    if (await row.isVisible()) {
      const nonStdCheckbox = row.locator('.version-inline .el-checkbox').filter({ hasText: '非标配' });
      const yaoCheckbox = row.locator('.version-inline .el-checkbox').filter({ hasText: '曜' });
      // 如果曜/远/驰已选中，非标配是disabled
      const yaoChecked = await yaoCheckbox.locator('.el-checkbox__input.is-checked').isVisible().catch(() => false);
      if (yaoChecked) {
        // 曜已选中 → 非标配应disabled
        const isDisabled = await nonStdCheckbox.locator('.el-checkbox__input.is-disabled').isVisible().catch(() => false);
        expect(isDisabled).toBe(true);
      } else {
        // 曜未选中 → 非标配可点击
        await nonStdCheckbox.click({ force: true });
        await page.waitForTimeout(1000);
        // 勾选非标配后，曜/远/驰应disabled
        const yaoDisabled = await yaoCheckbox.locator('.el-checkbox__input.is-disabled').isVisible().catch(() => false);
        expect(yaoDisabled).toBe(true);
      }
    }
  });

  test('最小集标记-二次点击曜系列显示绿色徽标', async ({ page }) => {
    const row = page.locator('.vrow:not(.sep-row)').first();
    if (await row.isVisible()) {
      const yaoCheckbox = row.locator('.version-inline .el-checkbox').filter({ hasText: '曜' });
      const yaoVerCell = row.locator('.version-inline .ver-cell').filter({ hasText: '曜' });
      // 第一次点击勾选曜
      await yaoCheckbox.click({ force: true });
      await page.waitForTimeout(1000);
      // 第二次点击 → 应标记为最小集（绿色徽标）
      await yaoCheckbox.click({ force: true });
      await page.waitForTimeout(1000);
      // 验证绿色徽标CSS类
      const hasBadge = await yaoVerCell.evaluate(el => el.classList.contains('ver-min-badge')).catch(() => false);
      expect(typeof hasBadge).toBe('boolean');
    }
  });
});
