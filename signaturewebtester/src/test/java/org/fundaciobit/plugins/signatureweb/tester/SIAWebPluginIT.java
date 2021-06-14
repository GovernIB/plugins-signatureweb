package org.fundaciobit.plugins.signatureweb.tester;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.HtmlFileInput;
import com.gargoylesoftware.htmlunit.util.WebConnectionWrapper;
import com.gargoylesoftware.htmlunit.util.WebResponseWrapper;
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

public class SIAWebPluginIT extends AbstractPluginIT {

    private WebDriver driver;

    @Before
    public void before() {

        driver = new HtmlUnitDriver(BrowserVersion.CHROME, true) {
            @Override
            protected WebClient modifyWebClient(WebClient client) {
                client.getOptions().setCssEnabled(false);

                // Workaround per: https://github.com/HtmlUnit/htmlunit/issues/357
                // La pàgina de SIA no retorna Content-Type
                new WebConnectionWrapper(client) {
                    public WebResponse getResponse(WebRequest request) throws IOException {
                        WebResponse response = super.getResponse(request);
                        if (response.getStatusCode() == 200 && response.getContentType().isEmpty()) {
                            response = new WebResponseWrapper(response) {
                                @Override
                                public String getContentType() {
                                    return "text/html";
                                }
                            };
                        }
                        return response;
                    }
                };

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

        driver.findElement(By.name("nif")).sendKeys(getConfig("sia.nif"));

        Select select = new Select(driver.findElement(By.name("pluginName")));
        select.selectByValue("miniappletinserversia");
        driver.findElement(By.name("pluginName")).click();
        driver.findElement(By.cssSelector("input[type='submit']")).submit();


        /* Triam el primer certificat
        driver.findElements(By.cssSelector("input[name=\"cert\"]")).get(0).click();
        driver.findElement(By.cssSelector(".btn-primary")).click();
        */

        driver.findElement(By.id("pin")).sendKeys(getConfig("sia.pin"));
        driver.findElement(By.id("btnFirmar")).click();

        Assert.assertEquals("2", driver.findElement(By.id("status")).getText());

        String link = driver.findElement(By.id("endSignLink")).getAttribute("href");
        Request.Get(link).execute().saveContent(new File("sia" + System.currentTimeMillis() + ".pdf"));
    }

}
