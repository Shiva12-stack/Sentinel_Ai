import { test, expect } from '@playwright/test'

test('observatory has only three navigation tabs and aggregate charts', async ({page}) => {
 await page.goto('/observatory')
 await expect(page.getByRole('link', {name:'Brand assets'})).toBeVisible()
 await expect(page.getByRole('link', {name:'Social & apps'})).toBeVisible()
 await expect(page.getByText('Risk distribution dashboard')).toBeVisible()
 await expect(page.getByText('Prioritized observation queue')).toHaveCount(0)
 await expect(page.getByText('Demo reset')).toHaveCount(0)
})
test('brand references are not displayed after registration', async ({page}) => {
 await page.goto('/observatory/brand')
 await expect(page.getByText('Reference registry')).toHaveCount(0)
 await expect(page.getByLabel('Platform (type any platform)')).toBeVisible()
})
test('social analysis requires a URL but does not require an official brand selection', async ({page}) => {
 await page.goto('/observatory/monitor')
 await expect(page.getByLabel('URL to analyze')).toBeVisible()
 await expect(page.getByRole('button', {name:'Analyze risk'})).toBeVisible()
 await expect(page.getByLabel('Brand', {exact: true})).toHaveCount(0)
 await expect(page.locator('.obs-json')).toHaveCount(0)
 await expect(page.getByText('Social & app findings')).toHaveCount(0)
})
