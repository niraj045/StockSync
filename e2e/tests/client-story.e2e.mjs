import fs from 'node:fs/promises';
import path from 'node:path';
import process from 'node:process';
import { Builder, By, until } from 'selenium-webdriver';
import chrome from 'selenium-webdriver/chrome.js';

const baseUrl = (process.env.E2E_BASE_URL ?? 'http://127.0.0.1:5173').replace(/\/$/, '');
const username = process.env.E2E_USERNAME ?? 'admin';
const password = process.env.E2E_PASSWORD ?? 'Password123';
const timeout = Number(process.env.E2E_TIMEOUT_MS ?? 20_000);
const headless = process.env.E2E_HEADLESS === 'true';
const artifacts = path.resolve('artifacts');

const options = new chrome.Options()
  .addArguments('--window-size=1440,1000')
  .addArguments('--disable-search-engine-choice-screen')
  .setUserPreferences({
    'credentials_enable_service': false,
    'profile.password_manager_enabled': false,
  });

if (headless) {
  options.addArguments('--headless=new');
}

const driver = await new Builder()
  .forBrowser('chrome')
  .setChromeOptions(options)
  .build();

async function visible(locator) {
  const element = await driver.wait(until.elementLocated(locator), timeout);
  await driver.wait(until.elementIsVisible(element), timeout);
  return element;
}

async function expectText(text) {
  await driver.wait(
    until.elementLocated(By.xpath(`//*[contains(normalize-space(.), ${xpathLiteral(text)})]`)),
    timeout,
    `Expected visible page text: ${text}`,
  );
}

function xpathLiteral(value) {
  if (!value.includes("'")) return `'${value}'`;
  if (!value.includes('"')) return `"${value}"`;
  return `concat('${value.replaceAll("'", "',\"'\",'")}')`;
}

async function openAndVerify(route, heading) {
  await driver.get(`${baseUrl}${route}`);
  await driver.wait(until.urlContains(route), timeout);
  await expectText(heading);
  console.log(`✓ ${heading}`);
}

try {
  console.log(`Running StockSync client story against ${baseUrl}`);

  // Security boundary: a quotation URL must redirect an anonymous browser.
  await driver.get(`${baseUrl}/quotations`);
  await driver.wait(until.urlContains('/login'), timeout);
  await expectText('Sign in to your workspace');
  console.log('✓ Protected route redirects to login');

  await (await visible(By.id('login_form_usernameOrEmail'))).sendKeys(username);
  await (await visible(By.id('login_form_password'))).sendKeys(password);
  await (await visible(By.css('button[type="submit"]'))).click();

  await driver.wait(async () => {
    const current = await driver.getCurrentUrl();
    return !current.includes('/login');
  }, timeout, 'Login did not leave the login page');
  await expectText('Shuttering Inventory Management');
  await expectText('Admin User');
  console.log('✓ ADMIN login succeeds');

  // Client-story navigation: master data, stock control, commercial work, and audit.
  await openAndVerify('/categories', 'Item categories');
  await openAndVerify('/items', 'Items');
  await openAndVerify('/parties', 'Parties');
  await openAndVerify('/sites', 'Sites');
  await openAndVerify('/inventory', 'Inventory');
  await openAndVerify('/opening-stock-imports', 'Opening Stock Import');
  await openAndVerify('/quotations', 'Quotations');
  await openAndVerify('/users', 'User');
  await openAndVerify('/audit-logs', 'Activity audit');

  console.log('\nPASS: StockSync client-story Selenium E2E completed.');
} catch (error) {
  await fs.mkdir(artifacts, { recursive: true });
  const screenshot = await driver.takeScreenshot();
  const screenshotPath = path.join(artifacts, `failure-${Date.now()}.png`);
  await fs.writeFile(screenshotPath, screenshot, 'base64');
  console.error(`\nFAIL: ${error.message}`);
  console.error(`Screenshot: ${screenshotPath}`);
  process.exitCode = 1;
} finally {
  await driver.quit();
}
