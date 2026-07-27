import fs from 'node:fs/promises';
import path from 'node:path';
import process from 'node:process';
import { Builder, By, Key, until } from 'selenium-webdriver';
import chrome from 'selenium-webdriver/chrome.js';

const baseUrl = (process.env.E2E_BASE_URL ?? 'http://127.0.0.1:5173').replace(/\/$/, '');
const username = process.env.E2E_USERNAME ?? 'admin';
const password = process.env.E2E_PASSWORD ?? 'Password123';
const timeout = Number(process.env.E2E_TIMEOUT_MS ?? 25_000);
const headless = process.env.E2E_HEADLESS === 'true';
const runId = Date.now().toString().slice(-8);
const data = {
  category: `E2E Shuttering Plates ${runId}`,
  itemCode: `E2E-PL-${runId}`,
  itemName: `E2E Steel Shuttering Plate ${runId}`,
  templateCode: `E2E-QT-${runId}`,
  templateName: `E2E Standard Quotation ${runId}`,
  party: `E2E Shree Ganesh Construction ${runId}`,
  siteCode: `E2E-GH-${runId}`,
  site: `E2E Ganesh Heights Phase 1 ${runId}`,
};
const artifacts = path.resolve('artifacts');

const options = new chrome.Options()
  .addArguments('--window-size=1600,1100')
  .addArguments('--disable-search-engine-choice-screen')
  .setUserPreferences({
    'credentials_enable_service': false,
    'profile.password_manager_enabled': false,
  });
if (headless) options.addArguments('--headless=new');

const driver = await new Builder().forBrowser('chrome').setChromeOptions(options).build();

function literal(value) {
  if (!value.includes("'")) return `'${value}'`;
  return `"${value}"`;
}

async function visible(locator) {
  const element = await driver.wait(until.elementLocated(locator), timeout);
  await driver.wait(until.elementIsVisible(element), timeout);
  return element;
}

async function pageContains(text) {
  return visible(By.xpath(`//*[contains(normalize-space(.), ${literal(text)})]`));
}

async function clickButton(text, scope) {
  const root = scope ?? driver;
  const locator = By.xpath(`.//button[.//span[normalize-space()=${literal(text)}] or normalize-space()=${literal(text)}]`);
  for (let i = 0; i < 5; i++) {
    try {
      const button = await driver.wait(async () => {
        const matches = await root.findElements(locator);
        for (const match of matches) {
          if (await match.isDisplayed()) return match;
        }
        return false;
      }, 3000);
      await driver.wait(until.elementIsEnabled(button), 3000);
      await button.click();
      return;
    } catch (err) {
      await driver.sleep(500);
    }
  }
  const button = await driver.wait(async () => {
    const matches = await root.findElements(locator);
    for (const match of matches) {
      if (await match.isDisplayed()) return match;
    }
    return false;
  }, timeout, `Visible button not found: ${text}`);
  await driver.wait(until.elementIsEnabled(button), timeout);
  await button.click();
}

async function clickRowButton(rowLocatorFn, buttonLocator) {
  for (let i = 0; i < 5; i++) {
    try {
      const row = await rowLocatorFn();
      const button = await row.findElement(buttonLocator);
      await driver.wait(until.elementIsEnabled(button), 2000);
      await driver.executeScript('arguments[0].click()', button);
      return;
    } catch (err) {
      await driver.sleep(500);
    }
  }
  const row = await rowLocatorFn();
  const button = await row.findElement(buttonLocator);
  await driver.executeScript('arguments[0].click()', button);
}

async function activeModal() {
  const locator = By.xpath(
    "//div[contains(@class,'ant-modal-content') and not(ancestor::div[contains(@style,'display: none')])]" +
    " | //div[contains(@class,'ant-drawer-content') and not(ancestor::div[contains(@style,'display: none')])]" +
    " | //div[contains(@class,'quotation-editor-page')]" +
    " | //section[contains(@class,'workflow-editor-page')]"
  );
  return visible(locator);
}

async function fieldContainer(label, scope) {
  const root = scope ?? driver;
  return root.findElement(By.xpath(
    `.//div[contains(@class,'ant-form-item')][.//label[normalize-space()=${literal(label)}]]`,
  ));
}

async function typeField(label, value, scope) {
  const container = await fieldContainer(label, scope);
  const input = await container.findElement(By.css('input, textarea'));
  await driver.executeScript("arguments[0].scrollIntoView({block: 'center'});", input);
  await input.sendKeys(Key.chord(Key.CONTROL, 'a'), String(value));
}

async function selectField(label, optionText, scope) {
  const container = await fieldContainer(label, scope);
  const selector = await container.findElement(By.css('.ant-select-selector'));
  await driver.executeScript("arguments[0].scrollIntoView({block: 'center'});", selector);
  await selector.click();
  await driver.sleep(300);
  await driver.executeScript(() => {
    const holders = document.querySelectorAll('.rc-virtual-list-holder');
    for (const h of holders) {
      h.scrollTop = h.scrollHeight;
    }
  });
  const option = await visible(By.xpath(
    `//div[contains(@class,'ant-select-item-option') and not(contains(@class,'ant-select-item-option-disabled'))]` +
    `[contains(normalize-space(.),${literal(optionText)})]`,
  ));
  await option.click();
}

async function saveModal(expectedText) {
  const modal = await activeModal();
  const submitBtn = await modal.findElement(By.css('button.ant-btn-primary'));
  await driver.wait(until.elementIsEnabled(submitBtn), timeout);
  await submitBtn.click();
  await waitUntilClosed(modal);
  await pageContains(expectedText);
}

async function waitUntilClosed(modal) {
  await driver.wait(async () => {
    try {
      return !(await modal.isDisplayed());
    } catch {
      return true;
    }
  }, timeout, 'Save modal did not close');
}

async function createCategory() {
  await driver.get(`${baseUrl}/categories`);
  await pageContains('Item categories');
  await clickButton('Add category');
  const modal = await activeModal();
  await typeField('Category name', data.category, modal);
  await typeField('Description', 'Daily-operation Selenium demonstration material', modal);
  await saveModal(data.category);
  console.log(`✓ Category created: ${data.category}`);
}

async function createTemplate() {
  await driver.get(`${baseUrl}/quotation-templates`);
  await pageContains('Quotation templates');
  await clickButton('Add template');
  const modal = await activeModal();
  await typeField('Template code', data.templateCode, modal);
  await typeField('Name', data.templateName, modal);
  await typeField('Company name', 'StockSync Shuttering Services', modal);
  await typeField('Company GSTIN', '27AABCS1234F1Z5', modal);
  await typeField('Company address', 'Pune, Maharashtra 411001', modal);
  await typeField('Default terms', 'Rental payable within seven days of invoice.', modal);
  await saveModal(data.templateCode);
  console.log(`✓ Quotation template created: ${data.templateCode}`);
}

async function createItem() {
  await driver.get(`${baseUrl}/items`);
  await pageContains('Item master');
  await clickButton('Add item');
  const modal = await activeModal();
  await typeField('Item code', data.itemCode, modal);
  await typeField('Item name', data.itemName, modal);
  await selectField('Category', data.category, modal);
  await typeField('Size', '3 ft x 2 ft', modal);
  await typeField('Unit', 'PCS', modal);
  await typeField('Purchase value', '2500', modal);
  await typeField('Rental configuration', 'Per piece per day', modal);
  await typeField('Loss rate', '1800', modal);
  await typeField('Scrap value', '500', modal);
  await typeField('Minimum stock', '10', modal);
  await saveModal(data.itemCode);
  console.log(`✓ Item created: ${data.itemCode}`);
}

async function createParty() {
  await driver.get(`${baseUrl}/parties`);
  await pageContains('Parties');
  await clickButton('Add party');
  const modal = await activeModal();
  await typeField('Legal name', data.party, modal);
  await typeField('Trade name', `Ganesh Construction ${runId}`, modal);
  await typeField('Contact person', 'Rajesh Patil', modal);
  await typeField('Phone', '9876543210', modal);
  await typeField('Email', `rajesh.${runId}@example.test`, modal);
  await typeField('Address', 'Office 12, MG Road, Pune, Maharashtra 411001', modal);
  await typeField('State', 'Maharashtra', modal);
  await typeField('Notes', 'Created by the Selenium daily-operation story', modal);
  await saveModal(data.party);
  console.log(`✓ Party created: ${data.party}`);
}

async function createSite() {
  await driver.get(`${baseUrl}/sites`);
  await pageContains('Sites');
  await clickButton('Add site');
  const modal = await activeModal();
  await selectField('Party', data.party, modal);
  await typeField('Site name', data.site, modal);
  await typeField('Site code', data.siteCode, modal);
  await typeField('Address', 'Wakad, Pune, Maharashtra 411057', modal);
  await typeField('Contact person', 'Amit Shinde', modal);
  await typeField('Start date', new Date().toISOString().slice(0, 10), modal);
  await selectField('Status', 'Active', modal);
  await typeField('Notes', 'Phase 1 daily-operation demonstration site', modal);
  await saveModal(data.siteCode);
  console.log(`✓ Site created: ${data.siteCode}`);
}

async function createAndApproveQuotation() {
  await driver.get(`${baseUrl}/quotations`);
  await pageContains('Quotations');
  await clickButton('Add quotation');
  const modal = await activeModal();
  await selectField('Template', data.templateCode, modal);
  await selectField('Party', data.party, modal);
  await selectField('Site', data.siteCode, modal);
  await selectField('Item', data.itemCode, modal);
  await typeField('Quantity', '100', modal);
  await typeField('Rate', '150', modal);
  await typeField('Transport', '1000', modal);
  await typeField('Loading', '500', modal);
  await typeField('Security deposit', '10000', modal);
  await typeField('Terms', 'Monthly rent payable within seven days of invoice.', modal);
  await typeField('Notes', `Daily-operation E2E quotation ${runId}`, modal);
  const submitBtn = await modal.findElement(By.css('button.ant-btn-primary'));
  await driver.wait(until.elementIsEnabled(submitBtn), timeout);
  await submitBtn.click();
  await driver.wait(async () => {
    const url = await driver.getCurrentUrl();
    return url.endsWith('/quotations');
  }, timeout, 'Quotation page did not redirect');
  await pageContains(data.party);

  const getQuotationRow = async () => visible(By.xpath(`//tr[.//td[contains(normalize-space(.),${literal(data.party)})]]`));
  const row = await getQuotationRow();
  const quotationNumber = (await row.findElements(By.css('td')))[0];
  const number = (await quotationNumber.getText()).trim();
  console.log(`✓ Draft quotation created: ${number}`);

  await clickRowButton(getQuotationRow, By.xpath(".//button[.//span[normalize-space()='Send']]"));
  await clickButton('OK');
  await driver.wait(async () => {
    try {
      return (await (await getQuotationRow()).getText()).includes('SENT');
    } catch {
      return false;
    }
  }, timeout);
  console.log('✓ Quotation sent');

  await clickRowButton(getQuotationRow, By.xpath(".//button[.//span[normalize-space()='Approve']]"));
  await clickButton('OK');
  await driver.wait(async () => {
    try {
      return (await (await getQuotationRow()).getText()).includes('APPROVED');
    } catch {
      return false;
    }
  }, timeout);
  await (await getQuotationRow()).findElement(By.xpath(".//button[.//span[normalize-space()='PDF']]"));
  console.log('✓ Quotation approved and PDF action is available');
  return number;
}

async function convertAndActivateAgreement(quotationNumber) {
  await driver.get(`${baseUrl}/agreements`);
  await pageContains('Agreements');
  await (await visible(By.css('[data-testid="convert-agreement"]'))).click();
  const modal = await activeModal();
  await (await modal.findElement(By.css('[data-testid="approved-quotation-select"] .ant-select-selector'))).click();
  await (await visible(By.xpath(
    `//div[contains(@class,'ant-select-item-option') and not(contains(@class,'ant-select-item-option-disabled'))]` +
    `[contains(normalize-space(.),${literal(quotationNumber)})]`
  ))).click();
  const submitBtn = await modal.findElement(By.css('button.ant-btn-primary'));
  await driver.wait(until.elementIsEnabled(submitBtn), timeout);
  await submitBtn.click();
  await waitUntilClosed(modal);

  // Close the automatically opened preview modal
  const previewModal = await activeModal();
  const closeBtn = await previewModal.findElement(By.css('.ant-modal-close'));
  await closeBtn.click();
  await waitUntilClosed(previewModal);

  await pageContains(data.party);
  const getAgreementRow = async () => visible(By.xpath(`//tr[.//td[contains(normalize-space(.),${literal(data.party)})]]`));
  const row = await getAgreementRow();
  const cells = await row.findElements(By.css('td'));
  const number = (await cells[0].getText()).trim();
  await clickRowButton(getAgreementRow, By.css('[data-testid="ready-agreement"]'));
  await clickButton('OK');
  await driver.wait(async () => {
    try {
      return (await (await getAgreementRow()).getText()).includes('READY FOR REVIEW');
    } catch {
      return false;
    }
  }, timeout);
  await clickRowButton(getAgreementRow, By.xpath(".//button[contains(normalize-space(.),'Generate PDF') or .//span[contains(normalize-space(.),'Generate PDF')]]"));
  await clickButton('OK');
  await driver.wait(async () => {
    try {
      return (await (await getAgreementRow()).findElements(By.xpath(".//button[contains(normalize-space(.),'PDF')]"))).length > 0;
    } catch {
      return false;
    }
  }, timeout);
  await clickRowButton(getAgreementRow, By.css('[data-testid="activate-agreement"]'));
  await clickButton('OK');
  await driver.wait(async () => {
    try {
      return (await (await getAgreementRow()).getText()).includes('ACTIVE');
    } catch {
      return false;
    }
  }, timeout);
  if ((await (await getAgreementRow()).findElements(By.css('[data-testid="edit-agreement"]'))).length) throw new Error('ACTIVE agreement is still editable');
  console.log(`✓ Agreement activated and read-only: ${number}`);
  return number;
}

try {
  console.log(`Running data-creating daily operation ${runId} against ${baseUrl}`);
  await driver.get(`${baseUrl}/login`);
  await driver.sleep(1500);
  await (await visible(By.id('login_form_usernameOrEmail'))).sendKeys(username);
  await (await visible(By.id('login_form_password'))).sendKeys(password);
  const loginBtn = await visible(By.css('button[type="submit"]'));
  await driver.executeScript('arguments[0].click()', loginBtn);
  await pageContains('Shuttering Inventory Management');
  console.log('✓ ADMIN login');

  await createCategory();
  await createTemplate();
  await createItem();
  await createParty();
  await createSite();
  const quotationNumber = await createAndApproveQuotation();
  const agreementNumber = await convertAndActivateAgreement(quotationNumber);

  await driver.get(`${baseUrl}/audit-logs`);
  await pageContains('Activity audit');
  await pageContains('QUOTATION');
  console.log('✓ Quotation audit activity is visible');

  console.log('\nPASS: Real-world daily operation completed.');
  console.log(JSON.stringify({ runId, quotationNumber, agreementNumber, ...data }, null, 2));
} catch (error) {
  await fs.mkdir(artifacts, { recursive: true });
  const screenshotPath = path.join(artifacts, `daily-operation-failure-${runId}.png`);
  await fs.writeFile(screenshotPath, await driver.takeScreenshot(), 'base64');
  console.error(`\nFAIL: ${error.message}\n${error.stack}`);
  console.error(`Screenshot: ${screenshotPath}`);
  try {
    const browserLogs = await driver.manage().logs().get('browser');
    for (const entry of browserLogs) console.error(`Browser: ${entry.message}`);
  } catch {
    // Browser logging is not available on every Chrome configuration.
  }
  process.exitCode = 1;
} finally {
  await driver.quit();
}
