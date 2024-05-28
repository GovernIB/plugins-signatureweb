package org.fundaciobit.pluginsib.signatureweb.tester;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.HtmlFileInput;
import com.gargoylesoftware.htmlunit.util.WebConnectionWrapper;
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

public class AutofirmaWebPluginIT extends AbstractPluginIT {

    private WebDriver driver;

    @Before
    public void before() {
        // per qualque motiu desconegut, emprant CHROME, no funciona bé les cridades que es fan a isfinished per
        // saber quan ha acabat la signature
        driver = new HtmlUnitDriver(BrowserVersion.FIREFOX_52, true) {
            @Override
            protected WebClient modifyWebClient(WebClient client) {
                client.getOptions().setCssEnabled(false);
                new WebConnectionWrapper(client) {
                    public WebResponse getResponse(WebRequest request) throws IOException {
                        // Capturam la URL de protocol que invocarà el JS de miniapplet
                        if (request.getUrl().getProtocol().equals("afirma")) {
                            String command = getConfig("autofirma.command");
                            String arg = request.getUrl().toString();
                            Process autofirmaProcess = new ProcessBuilder().inheritIO()
                                    .command(command, arg)
                                    .start();

                            try {
                                autofirmaProcess.waitFor();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }
                        return super.getResponse(request);
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
        if (fitxerElement instanceof HtmlUnitWebElement) {
            HtmlFileInput htmlFileInput = (HtmlFileInput) ((HtmlUnitWebElement) fitxerElement).getAuxiliary();
            htmlFileInput.setContentType("application/pdf");
        }

        driver.findElement(By.name("nif")).sendKeys(getConfig("autofirma.nif"));

        Select select = new Select(driver.findElement(By.name("pluginName")));
        select.selectByValue("autofirma");
        driver.findElement(By.name("pluginName")).click();
        driver.findElement(By.cssSelector("input[type='submit']")).submit();

        Assert.assertEquals("2", driver.findElement(By.id("status")).getText());

        String link = driver.findElement(By.id("endSignLink")).getAttribute("href");
        Request.Get(link).execute().saveContent(new File("autofirma" + System.currentTimeMillis() + ".pdf"));
    }

    @Test
    public void testSignPdfTimestamp() throws URISyntaxException, IOException {
        driver.get(endpoint);

        WebElement fitxerElement = driver.findElement(By.name("fitxer"));
        File localFile = new File(getClass().getResource("/normal.pdf").toURI());
        fitxerElement.sendKeys(localFile.getAbsolutePath());
        if (fitxerElement instanceof HtmlUnitWebElement) {
            HtmlFileInput htmlFileInput = (HtmlFileInput) ((HtmlUnitWebElement) fitxerElement).getAuxiliary();
            htmlFileInput.setContentType("application/pdf");
        }

        driver.findElement(By.name("nif")).sendKeys(getConfig("autofirma.nif"));

        Select select = new Select(driver.findElement(By.name("pluginName")));
        select.selectByValue("autofirma");

        driver.findElement(By.name("pluginName")).click();
        driver.findElement(By.name("timestamp")).click();

        driver.findElement(By.cssSelector("input[type='submit']")).submit();

        Assert.assertEquals("2", driver.findElement(By.id("status")).getText());

        String link = driver.findElement(By.id("endSignLink")).getAttribute("href");
        Request.Get(link).execute().saveContent(new File("autofirma_ts" + System.currentTimeMillis() + ".pdf"));
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

        driver.findElement(By.name("nif")).sendKeys(getConfig("autofirma.nif"));

        Select select = new Select(driver.findElement(By.name("pluginName")));
        select.selectByValue("autofirma");
        driver.findElement(By.name("pluginName")).click();
        driver.findElement(By.cssSelector("input[type='submit']")).submit();

        Assert.assertEquals("2", driver.findElement(By.id("status")).getText());

        String link = driver.findElement(By.id("endSignLink")).getAttribute("href");
        Request.Get(link).execute().saveContent(new File("autofirma" + System.currentTimeMillis() + ".xsig"));
    }
}
