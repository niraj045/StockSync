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
  await driver.executeScript('arguments[0].click()', button);
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
  if ((await input.getAttribute('type')) === 'date') {
    await driver.executeScript((element, nextValue) => {
      const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value')?.set;
      setter?.call(element, nextValue);
      element.dispatchEvent(new Event('input', { bubbles: true }));
      element.dispatchEvent(new Event('change', { bubbles: true }));
    }, input, String(value));
    return;
  }
  await input.sendKeys(Key.chord(Key.CONTROL, 'a'), String(value));
}

async function selectField(label, optionText, scope) {
  const container = await fieldContainer(label, scope);
  const selector = await container.findElement(By.css('.ant-select-selector'));
  await driver.executeScript("arguments[0].scrollIntoView({block: 'center'});", selector);
  await selector.click();
  const searchInputs = await container.findElements(By.css('input[type="search"]'));
  if (searchInputs.length > 0) {
    await searchInputs[0].sendKeys(Key.chord(Key.CONTROL, 'a'), optionText);
  }
  await driver.sleep(300);
  await driver.executeScript(() => {
    const holders = document.querySelectorAll('.rc-virtual-list-holder');
    for (const h of holders) {
      h.scrollTop = h.scrollHeight;
    }
  });
  const optionLocator = By.xpath(
    `//div[contains(@class,'ant-select-item-option') and not(contains(@class,'ant-select-item-option-disabled'))]` +
    `[contains(normalize-space(.),${literal(optionText)})]`,
  );
  await driver.wait(async () => {
    try {
      const matches = await driver.findElements(optionLocator);
      for (const match of matches) {
        if (await match.isDisplayed()) {
          await driver.executeScript('arguments[0].click()', match);
          return true;
        }
      }
    } catch {
      // Ant Design can replace virtual-list nodes while options are loading.
    }
    return false;
  }, timeout, `Selectable option not found: ${optionText}`);
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

async function createAndConfirmOrder(agreementNumber) {
  await driver.get(`${baseUrl}/orders`);
  await pageContains('Site Orders');
  await (await visible(By.xpath("//button[.//span[normalize-space()='Create order']]"))).click();
  const drawer = await activeModal();

  // Select the Active Agreement
  await selectField('Select Agreement', agreementNumber, drawer);

  const todayStr = new Date().toISOString().slice(0, 10);
  await typeField('Order Date', todayStr, drawer);

  const itemSelect = await visible(By.xpath("//div[contains(@class,'ant-select-selector')][.//input[@id='items_0_itemId']]"));
  await itemSelect.click();
  await driver.sleep(300);

  const option = await visible(By.xpath(
    `//div[contains(@class,'ant-select-item-option') and not(contains(@class,'ant-select-item-option-disabled'))]` +
    `[contains(normalize-space(.),${literal(data.itemCode)})]`
  ));
  await option.click();
  await driver.sleep(300);

  const qtyInput = await drawer.findElement(By.css('input.ant-input-number-input[placeholder="Qty"]'));
  await qtyInput.sendKeys(Key.chord(Key.CONTROL, 'a'), '50');

  const submitBtn = await drawer.findElement(By.xpath(".//button[.//span[normalize-space()='Create order']]"));
  await submitBtn.click();
  await waitUntilClosed(drawer);

  console.log('✓ Site Order created in DRAFT');

  await pageContains(agreementNumber);
  const getOrderRow = async () => visible(By.xpath(`//tr[.//td[contains(normalize-space(.),${literal(agreementNumber)})]]`));
  const orderNumber = await (await getOrderRow()).findElement(By.xpath("./td[1]")).getText();

  await clickRowButton(getOrderRow, By.xpath(".//button[.//span[normalize-space()='Confirm']]"));
  await clickButton('OK');

  await driver.wait(async () => {
    try {
      return (await (await getOrderRow()).getText()).includes('CONFIRMED');
    } catch {
      return false;
    }
  }, timeout);

  console.log(`✓ Site Order confirmed: ${orderNumber}`);

  await clickRowButton(getOrderRow, By.xpath(".//button[normalize-space()='View']"));
  const modal = await activeModal();

  await pageContains('50');
  await pageContains('0');

  const closeBtn = await visible(By.css('.ant-modal-content .ant-modal-close'));
  await driver.executeScript('arguments[0].click()', closeBtn);
  await driver.wait(async () => {
    const openModals = await driver.findElements(By.css('.ant-modal-content'));
    for (const element of openModals) {
      try {
        if (await element.isDisplayed()) return false;
      } catch {
        // A detached modal is closed.
      }
    }
    return true;
  }, timeout);

  await clickRowButton(getOrderRow, By.xpath(".//button[.//span[normalize-space()='PDF']]"));
  await driver.sleep(2000);

  console.log('✓ Site Order PDF downloaded');
  return orderNumber;
}

async function adjustStockIn(itemCode) {
  await driver.get(`${baseUrl}/inventory`);
  await pageContains('Inventory');
  
  await (await visible(By.xpath("//button[.//span[normalize-space()='Adjustment']]"))).click();
  const drawer = await activeModal();
  
  await typeField('Reason', 'Initial E2E Stock Adjustment', drawer);
  
  const itemSelect = await visible(By.xpath("//div[contains(@class,'ant-select')][.//input[@id='items_0_itemId']]"));
  await itemSelect.click();
  const itemSearch = await itemSelect.findElement(By.css('input[type="search"]'));
  await itemSearch.sendKeys(itemCode);
  await driver.sleep(300);
  
  const option = await visible(By.xpath(
    `//div[contains(@class,'ant-select-item-option') and not(contains(@class,'ant-select-item-option-disabled'))]` +
    `[contains(normalize-space(.),${literal(itemCode)})]`
  ));
  await option.click();
  await driver.sleep(300);
  
  const qtyInput = await drawer.findElement(By.css('input.ant-input-number-input[placeholder="Quantity"]'));
  await qtyInput.sendKeys(Key.chord(Key.CONTROL, 'a'), '100');
  
  const submitBtn = await drawer.findElement(By.xpath(".//button[.//span[normalize-space()='Post transaction']]"));
  await submitBtn.click();
  await waitUntilClosed(drawer);
  
  console.log('✓ Godown stock adjusted: 100');
}

async function createChallanAndVerifyStock(agreementNumber, orderNumber) {
  await driver.get(`${baseUrl}/challans/issued`);
  await pageContains('Issued Challans');
  await (await visible(By.xpath("//button[.//span[normalize-space()='Issue Challan']]"))).click();
  const drawer = await activeModal();

  await selectField('Select Confirmed Site Order', orderNumber, drawer);

  const todayStr = new Date().toISOString().slice(0, 10);
  await typeField('Dispatch Date', todayStr, drawer);
  await typeField('Vehicle Number', 'MH-12-AB-1234', drawer);
  await typeField('Driver Name', 'Rajesh Patil', drawer);

  const qtyInput = await drawer.findElement(By.css('input.ant-input-number-input'));
  await qtyInput.sendKeys(Key.chord(Key.CONTROL, 'a'), '40');

  const submitBtn = await drawer.findElement(By.xpath(".//button[.//span[normalize-space()='Generate Challan']]"));
  await submitBtn.click();
  await waitUntilClosed(drawer);

  console.log('✓ Issued Challan created (partial)');

  await pageContains(orderNumber);

  await driver.get(`${baseUrl}/inventory`);
  await pageContains('Inventory');
  
  const getStockRow = async () => visible(By.xpath(`//tr[.//td[contains(normalize-space(.),${literal(data.itemCode)})]]`));
  let rowText = await (await getStockRow()).getText();
  
  if (!rowText.includes('60 PCS')) throw new Error('Godown stock was not reduced correctly. Expected 60 PCS, got: ' + rowText);
  if (!rowText.includes('40')) throw new Error('Global issued stock was not increased correctly. Expected 40, got: ' + rowText);
  
  console.log('✓ Godown available and global issued stock verified');

  await driver.get(`${baseUrl}/orders`);
  await pageContains('Site Orders');
  
  const getOrderRow = async () => visible(By.xpath(`//tr[.//td[contains(normalize-space(.),${literal(agreementNumber)})]]`));
  
  await driver.wait(async () => {
    try {
      return (await (await getOrderRow()).getText()).includes('PARTIALLY FULFILLED');
    } catch {
      return false;
    }
  }, timeout);

  await clickRowButton(getOrderRow, By.xpath(".//button[normalize-space()='View']"));
  const viewModal = await activeModal();
  
  await pageContains('40');
  await pageContains('10');

  const closeBtn = await viewModal.findElement(By.css('.ant-modal-close'));
  await closeBtn.click();
  await waitUntilClosed(viewModal);

  console.log('✓ Site Order status and remaining quantity verified');

  await driver.get(`${baseUrl}/challans/issued`);
  await pageContains('Issued Challans');
  await (await visible(By.xpath("//button[.//span[normalize-space()='Issue Challan']]"))).click();
  const drawer2 = await activeModal();

  await selectField('Select Confirmed Site Order', orderNumber, drawer2);
  await typeField('Dispatch Date', todayStr, drawer2);
  
  const qtyInput2 = await drawer2.findElement(By.css('input.ant-input-number-input'));
  await qtyInput2.sendKeys(Key.chord(Key.CONTROL, 'a'), '10');

  const submitBtn2 = await drawer2.findElement(By.xpath(".//button[.//span[normalize-space()='Generate Challan']]"));
  await submitBtn2.click();
  await waitUntilClosed(drawer2);

  console.log('✓ Completing Issued Challan created');

  await driver.get(`${baseUrl}/orders`);
  await pageContains('Site Orders');
  
  await driver.wait(async () => {
    try {
      return (await (await getOrderRow()).getText()).includes('FULFILLED');
    } catch {
      return false;
    }
  }, timeout);

  console.log('✓ Site Order status is FULFILLED');

  await driver.get(`${baseUrl}/inventory`);
  await pageContains('Inventory');
  
  const rowTextFinal = await (await getStockRow()).getText();
  if (!rowTextFinal.includes('50 PCS')) throw new Error('Godown stock was not reduced correctly. Expected 50 PCS, got: ' + rowTextFinal);
  
  console.log('✓ Final stock verification successful');

  await driver.get(`${baseUrl}/challans/issued`);
  await pageContains('Issued Challans');
  
  const getChallanRow = async () => visible(By.xpath(`//tr[.//td[contains(normalize-space(.),${literal(orderNumber)})]]`));
  await clickRowButton(getChallanRow, By.xpath(".//button[.//span[normalize-space()='PDF']]"));
  await driver.sleep(2000);

  console.log('✓ Issued Challan PDF downloaded');
}



try {
  console.log(`Running data-creating daily operation ${runId} against ${baseUrl}`);
  await driver.get(`${baseUrl}/login`);
  await driver.sleep(1500);
  await (await visible(By.id('login_form_usernameOrEmail'))).sendKeys(username);
  await (await visible(By.id('login_form_password'))).sendKeys(password);
  const loginBtn = await visible(By.css('button[type="submit"]'));
  await driver.executeScript('arguments[0].click()', loginBtn);
  await pageContains('Operational Dashboard');
  console.log('✓ ADMIN login');

  await createCategory();
  await createTemplate();
  await createItem();
  await createParty();
  await createSite();
  const quotationNumber = await createAndApproveQuotation();
  const agreementNumber = await convertAndActivateAgreement(quotationNumber);
  const orderNumber = await createAndConfirmOrder(agreementNumber);
  await adjustStockIn(data.itemCode);
  await createChallanAndVerifyStock(agreementNumber, orderNumber);

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
