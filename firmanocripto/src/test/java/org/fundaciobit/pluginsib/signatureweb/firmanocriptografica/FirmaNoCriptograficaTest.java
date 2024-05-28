package org.fundaciobit.pluginsib.signatureweb.firmanocriptografica;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


/**
 * 
 * @author anadal
 *
 */
public class FirmaNoCriptograficaTest extends TestCase {
    /**
     * Create the test case
     *
     * @param testName
     *          name of the test case
     */
    public FirmaNoCriptograficaTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(FirmaNoCriptograficaTest.class);
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp() {
        assertTrue(true);
    }

    public static void main(String[] args) {

        try {

            System.out.println("FINAL PROCESS DE FIRMA OK");

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
