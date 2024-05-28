package org.fundaciobit.pluginsib.signatureweb.fortress.test;

import com.viafirma.fortress.sdk.FortressApi;
import com.viafirma.fortress.sdk.configuration.FortressApiConfiguration;
import com.viafirma.fortress.sdk.exception.ApiException;
import com.viafirma.fortress.sdk.model.AccessToken;
import com.viafirma.fortress.sdk.model.UserStatus;

import org.jboss.logging.Logger;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static com.viafirma.fortress.sdk.FortressApi.GRANT_TYPE_CLIENT_CREDENTIALS;

import java.io.FileInputStream;
import java.util.Properties;

/**
 * 
 * @author anadal
 *
 */
public class TestFortress {

    private static final Logger log = Logger.getLogger(TestFortress.class);

    private static FortressApi api;

    private static Properties properties;

    @BeforeClass
    public static void setup() throws Exception {

        Properties prop = new Properties();
        prop.load(new FileInputStream("test.properties"));

        properties = prop;

        String ENDPOINT = properties.getProperty("endpoint");
        String CLIENT_ID = properties.getProperty("client.id");
        String CLIENT_SECRET = properties.getProperty("client.secret");

        FortressApiConfiguration conf = new FortressApiConfiguration(ENDPOINT, CLIENT_ID, CLIENT_SECRET);
        conf.setDebug(true);
        api = new FortressApi(conf);
    }

    @Test
    @Ignore
    public void testAuth() throws Exception {

        AccessToken accessToken = api.getAccessToken("", properties.getProperty("portafib"),
                GRANT_TYPE_CLIENT_CREDENTIALS);

        log.info(accessToken.getAccessToken());
        log.info(accessToken.getExpiresIn());

        String dni = properties.getProperty("dni");
        checkUser(accessToken, "DNI_QUE_NO_EXISTEIX");
        checkUser(accessToken, dni);
    }

    protected void checkUser(AccessToken accessToken, String dni) throws ApiException {
        try {

            UserStatus us = api.getUserStatus(accessToken.getAccessToken(), dni);
            log.info("User Status => Hem trobat l'usuari " + dni);
            log.info("User Status => us.isAuth(): " + us.isAuth());
            log.info("User Status => us.isSign(): " + us.isSign());

        } catch (com.viafirma.fortress.sdk.exception.UserNotFoundException userNotFoundException) {
            log.error("User Status => NO hem trobat l'usuari " + dni);
        }
    }

    public static void main(String[] args) {

        try {
            TestFortress tf = new TestFortress();
            TestFortress.setup();
            tf.testAuth();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
