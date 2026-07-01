import { BasePage } from './base.page';
import { Locator, Page } from '@playwright/test';
import { ROUTES } from '../fixtures/test-data';

export class RequirementImagesPage extends BasePage {
  readonly headerTitle: Locator;
  readonly galleryBody: Locator;

  constructor(page: Page) {
    super(page);
    this.headerTitle = page.getByText('需求图片');
    this.galleryBody = page.locator('.gallery-body');
  }

  async goto() {
    await this.navigateTo(ROUTES.requirementImages);
  }
}
