package fedora.test;

import junit.framework.Test;
import junit.framework.TestSuite;
import fedora.test.integration.TestAPIALite;

/**
 * @author Edwin Shin
 */
public class SuiteFoo {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(SuiteFoo.suite());
    }

    public static Test suite() {
        TestSuite suite = new TestSuite("Test for fedora.test.junit");
        //$JUnit-BEGIN$
        suite.addTestSuite(TestFoo.class);
        suite.addTest(TestAPIALite.suite());
        suite.addTest(SuiteBar.suite());
        //$JUnit-END$
        
        return new FedoraServerTestSetup(suite);
    }
}