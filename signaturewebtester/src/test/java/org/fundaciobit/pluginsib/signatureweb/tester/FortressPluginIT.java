package org.fundaciobit.pluginsib.signatureweb.tester;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlFileInput;
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
import java.util.concurrent.TimeUnit;

public class FortressPluginIT extends AbstractPluginIT {

    private WebDriver driver;

    @Before
    public void before() {
        driver = new HtmlUnitDriver(BrowserVersion.CHROME, true) {
            @Override
            protected WebClient modifyWebClient(WebClient client) {
                client.getOptions().setCssEnabled(false);
                return client;
            }
        };
        driver.manage().timeouts().implicitlyWait(15, TimeUnit.SECONDS);
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

        driver.findElement(By.name("nif")).sendKeys(getConfig("fortress.nif"));

        Select select = new Select(driver.findElement(By.name("pluginName")));
        select.selectByValue("fortress");
        driver.findElement(By.name("pluginName")).click();
        driver.findElement(By.cssSelector("input[type='submit']")).submit();

        driver.findElement(By.id("verify-code")).sendKeys(getConfig("fortress.pin"));

        // Woraround. Amb Vaddin (el framework que empra fortress) no va bé només fer click
        // cal fer un dblClick, i per fer-lo cal emprar l'api interna de htmlunit.
        WebElement element = driver.findElement(By.id("verify-button"));
        ((HtmlElement) ((HtmlUnitWebElement) element).getAuxiliary()).dblClick();

        Assert.assertEquals("2", driver.findElement(By.id("status")).getText());

        String link = driver.findElement(By.id("endSignLink")).getAttribute("href");
        Request.Get(link).execute().saveContent(new File("fortress" + System.currentTimeMillis() + ".pdf"));
    }

    @Test
    public void testSignXml() throws URISyntaxException, IOException {
        driver.get(endpoint);

        WebElement fitxerElement = driver.findElement(By.name("fitxer"));
        File localFile = new File(getClass().getResource("/sample.xml").toURI());
        fitxerElement.sendKeys(localFile.getAbsolutePath());

        // Per passar el content-type cal accedir a l'api interna de htmlunit
        HtmlFileInput htmlFileInput = (HtmlFileInput) ((HtmlUnitWebElement) fitxerElement).getAuxiliary();
        htmlFileInput.setContentType("text/xml");

        driver.findElement(By.name("nif")).sendKeys(getConfig("fortress.nif"));

        Select select = new Select(driver.findElement(By.name("pluginName")));
        select.selectByValue("fortress");
        driver.findElement(By.name("pluginName")).click();
        driver.findElement(By.cssSelector("input[type='submit']")).submit();

        driver.findElement(By.id("verify-code")).sendKeys(getConfig("fortress.pin"));

        // Woraround. Amb Vaddin (el framework que empra fortress) no va bé només fer click
        // cal fer un dblClick, i per fer-lo cal emprar l'api interna de htmlunit.
        WebElement element = driver.findElement(By.id("verify-button"));
        ((HtmlElement) ((HtmlUnitWebElement) element).getAuxiliary()).dblClick();

        Assert.assertEquals("2", driver.findElement(By.id("status")).getText());

        String link = driver.findElement(By.id("endSignLink")).getAttribute("href");
        Request.Get(link).execute().saveContent(new File("fortress" + System.currentTimeMillis() + ".xsig"));
    }

}
