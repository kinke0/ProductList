import { test, expect } from '@playwright/test';
import { BaseDataPage } from '../../pages/base-data.page';

test.describe('基础数据维护-产品分类', () => {
  let baseDataPage: BaseDataPage;

  test.beforeEach(async ({ page }) => {
    baseDataPage = new BaseDataPage(page);
    await baseDataPage.gotoCategory();
  });

  test('页面加载-标题可见', async () => {
    await expect(baseDataPage.headerTitle).toBeVisible();
  });

  test('产品分类表格可见', async () => {
    await expect(baseDataPage.dataTable).toBeVisible();
  });
});

test.describe('基础数据维护-解决方案', () => {
  let baseDataPage: BaseDataPage;

  test.beforeEach(async ({ page }) => {
    baseDataPage = new BaseDataPage(page);
    await baseDataPage.gotoSolution();
  });

  test('页面加载-标题可见', async () => {
    await expect(baseDataPage.headerTitle).toBeVisible();
  });

  test('选项表格可见', async () => {
    await expect(baseDataPage.dataTable).toBeVisible();
  });
});

test.describe('基础数据维护-功能状态', () => {
  let baseDataPage: BaseDataPage;

  test.beforeEach(async ({ page }) => {
    baseDataPage = new BaseDataPage(page);
    await baseDataPage.gotoStatus();
  });

  test('页面加载-标题可见', async () => {
    await expect(baseDataPage.headerTitle).toBeVisible();
  });

  test('选项表格可见', async () => {
    await expect(baseDataPage.dataTable).toBeVisible();
  });
});
