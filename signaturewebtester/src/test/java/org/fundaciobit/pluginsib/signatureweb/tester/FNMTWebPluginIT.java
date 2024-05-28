package org.fundaciobit.pluginsib.signatureweb.tester;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlFileInput;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import org.apache.http.client.fluent.Request;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.htmlunit.HtmlUnitWebElement;
import org.openqa.selenium.support.ui.Select;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.concurrent.TimeUnit;

public class FNMTWebPluginIT extends AbstractPluginIT {

    private WebDriver driver;

    @Before
    public void before() {

        driver = new HtmlUnitDriver(BrowserVersion.CHROME, true) {
            @Override
            protected WebClient modifyWebClient(WebClient client) {
                client.getOptions().setCssEnabled(false);
                client.getOptions().setThrowExceptionOnScriptError(false);
                return client;
            }
        };

        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
        driver.manage().timeouts().setScriptTimeout(10, TimeUnit.SECONDS);
    }

    @After
    public void after() {
        driver.quit();
    }

    @Test
    public void testSignPdf() throws URISyntaxException, IOException {
        driver.get(endpoint);

        WebElement fitxerElement = driver.findElement(By.name("fitxer"));
        File localFile = new File(getClass().getResource("/normal.pdf").toURI());
        fitxerElement.sendKeys(localFile.getAbsolutePath());

        // Per passar el content-type cal accedir a l'api interna de htmlunit
        HtmlFileInput htmlFileInput = (HtmlFileInput) ((HtmlUnitWebElement) fitxerElement).getAuxiliary();
        htmlFileInput.setContentType("application/pdf");

        driver.findElement(By.name("nif")).sendKeys(getConfig("fnmtcloud.nif"));

        Select select = new Select(driver.findElement(By.name("pluginName")));
        select.selectByValue("fnmtcloud");
        driver.findElement(By.name("pluginName")).click();
        driver.findElement(By.cssSelector("input[type='submit']")).submit();

        driver.findElement(By.cssSelector("#urn\\:fnmt\\:authentication\\:flow\\:password_totp > a")).click();

        // Autenticació per Usuari i password
        driver.findElement(By.id("idAttributeInput")).sendKeys(getConfig("fnmtcloud.nif"));
        driver.findElement(By.id("passwordInput")).sendKeys(getConfig("fnmtcloud.password"));
        driver.findElement(By.id("passwordForm-submitButton")).click();

        // Autenticació per Time-based One-Time Password
        GoogleAuthenticator gAuth = new GoogleAuthenticator();
        int code = gAuth.getTotpPassword(getConfig("fnmtcloud.secret"));
        System.out.println(code);
        driver.findElement(By.id("totp")).sendKeys(Integer.toString(code));
        driver.findElement(By.id("send")).click();

        // Marcar que hem llegit el document i continuar
        driver.findElement(By.id("readCheckbox")).click();
        driver.findElement(By.id("continueButton")).click();

        // Selector del PIN
        String pin = getConfig("fnmtcloud.pin");
        MessageFormat messageFormat = new MessageFormat("input[type=\"button\"][value=\"{0}\"].btn-lg");
        Object[] params = new Object[1];
        for (int i = 0; i < pin.length(); i++) {
            params[0] = pin.charAt(i);
            String selector = messageFormat.format(params);
            driver.findElement(By.cssSelector(selector)).click();
        }
        driver.findElement(By.id("pinCodeForm-submitButton")).click();

        Assert.assertEquals("2", driver.findElement(By.id("status")).getText());

        String link = driver.findElement(By.id("endSignLink")).getAttribute("href");
        Request.Get(link).execute().saveContent(new File("fnmt" + System.currentTimeMillis() + ".pdf"));
    }

}
