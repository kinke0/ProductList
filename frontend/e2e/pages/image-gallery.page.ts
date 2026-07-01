import { BasePage } from './base.page';
import { Locator, Page } from '@playwright/test';
import { ROUTES } from '../fixtures/test-data';

export class ImageGalleryPage extends BasePage {
  readonly headerTitle: Locator;
  readonly galleryContent: Locator;

  constructor(page: Page) {
    super(page);
    this.headerTitle = page.getByText('图床管理');
    this.galleryContent = page.locator('.page');
  }

  async goto() {
    await this.navigateTo(ROUTES.imageGallery);
  }
}
