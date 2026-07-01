import { BasePage } from './base.page';
import { Locator, Page } from '@playwright/test';

export class PanoramaPage extends BasePage {
  // 全景图区域
  readonly panoramaTab: Locator;
  readonly loadingIndicator: Locator;
  readonly emptyState: Locator;

  // 图表区域
  readonly chartFixedTop: Locator;

  // 状态栏
  readonly statusLegendBar: Locator;
  readonly legendItems: Locator;

  // 滚动区域
  readonly panoramaScroll: Locator;
  readonly sections: Locator;
  readonly sectionTitles: Locator;

  // L1 卡片
  readonly l1Cards: Locator;
  readonly l1Headers: Locator;

  // L2 标签
  readonly l2Tags: Locator;

  // L3 卡片
  readonly l3Cards: Locator;

  constructor(page: Page) {
    super(page);
    this.panoramaTab = page.locator('.panorama-tab');
    this.loadingIndicator = page.locator('.panorama-loading');
    this.emptyState = page.locator('.el-empty');

    this.chartFixedTop = page.locator('.chart-fixed-top');
    this.statusLegendBar = page.locator('.status-legend-bar');
    this.legendItems = page.locator('.legend-item');

    this.panoramaScroll = page.locator('.panorama-scroll');
    this.sections = page.locator('.panorama-section');
    this.sectionTitles = page.locator('.section-title');

    this.l1Cards = page.locator('.l1-card');
    this.l1Headers = page.locator('.l1-header');

    this.l2Tags = page.locator('.l2-tag');
    this.l3Cards = page.locator('.l3-card');
  }

  async getSectionCount(): Promise<number> {
    return await this.sections.count();
  }

  async getL1CardCount(): Promise<number> {
    return await this.l1Cards.count();
  }

  async getL3CardCount(): Promise<number> {
    return await this.l3Cards.count();
  }

  async clickL1Header(l1Name: string) {
    const header = this.l1Headers.filter({ hasText: l1Name }).first();
    await header.click();
  }

  async clickL2Tag(l2Name: string) {
    const tag = this.l2Tags.filter({ hasText: l2Name }).first();
    await tag.click();
  }

  async clickL3Card(l3Name: string) {
    const card = this.l3Cards.filter({ hasText: l3Name }).first();
    await card.click();
  }

  async getSectionTitleText(index: number): Promise<string> {
    const title = this.sectionTitles.nth(index);
    return await title.textContent() || '';
  }

  async getLegendItemTexts(): Promise<string[]> {
    const items = this.legendItems;
    const count = await items.count();
    const texts: string[] = [];
    for (let i = 0; i < count; i++) {
      texts.push(await items.nth(i).textContent() || '');
    }
    return texts;
  }
}
