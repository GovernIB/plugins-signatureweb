package org.fundaciobit.pluginsib.signatureweb.tester;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
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

public class FIReSignatureWebPluginIT extends AbstractPluginIT {

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
        if (fitxerElement instanceof HtmlUnitWebElement) {
            HtmlFileInput htmlFileInput = (HtmlFileInput) ((HtmlUnitWebElement) fitxerElement).getAuxiliary();
            htmlFileInput.setContentType("application/pdf");
        }

        driver.findElement(By.name("nif")).sendKeys(getConfig("fire.nif"));

        Select select = new Select(driver.findElement(By.name("pluginName")));
        select.selectByValue("fire");
        driver.findElement(By.name("pluginName")).click();
        driver.findElement(By.cssSelector("input[type='submit']")).submit();

        driver.findElement(By.cssSelector("a.button")).click();
        driver.findElement(By.id("pin")).sendKeys(getConfig("fire.pin"));
        driver.findElement(By.cssSelector("button[type='submit']")).submit();

        Assert.assertEquals("2", driver.findElement(By.id("status")).getText());

        String link = driver.findElement(By.id("endSignLink")).getAttribute("href");
        Request.Get(link).execute().saveContent(new File("fire" + System.currentTimeMillis() + ".pdf"));
    }

    @Test
    public void testSignXml() throws URISyntaxException, IOException {
        driver.get(endpoint);

        WebElement fitxerElement = driver.findElement(By.name("fitxer"));
        File localFile = new File(getClass().getResource("/sample.xml").toURI());
        fitxerElement.sendKeys(localFile.getAbsolutePath());
        if (fitxerElement instanceof HtmlUnitWebElement) {
            HtmlFileInput htmlFileInput = (HtmlFileInput) ((HtmlUnitWebElement) fitxerElement).getAuxiliary();
            htmlFileInput.setContentType("text/xml");
        }

        driver.findElement(By.name("nif")).sendKeys(getConfig("fire.nif"));

        Select select = new Select(driver.findElement(By.name("pluginName")));
        select.selectByValue("fire");
        driver.findElement(By.name("pluginName")).click();
        driver.findElement(By.cssSelector("input[type='submit']")).submit();

        driver.findElement(By.cssSelector("a.button")).click();
        driver.findElement(By.id("pin")).sendKeys(getConfig("fire.pin"));
        driver.findElement(By.cssSelector("button[type='submit']")).submit();

        Assert.assertEquals("2", driver.findElement(By.id("status")).getText());

        String link = driver.findElement(By.id("endSignLink")).getAttribute("href");
        Request.Get(link).execute().saveContent(new File("fire" + System.currentTimeMillis() + ".xsig"));
    }

}
