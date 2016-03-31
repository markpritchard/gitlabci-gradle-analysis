package test;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

public class BrokenUnitTest {
    private void method1() {
        new File("/tmp").canRead();
        switch ('a') {
            case 'b':
                System.out.println("b");
        }
    }

    @Test
    public void testFail1() {
        System.out.println("sout: testFail1");
        System.err.println("serr: testFail1");
        Assert.fail("This test fails.");
    }

    @Test
    public void testFail2() {
        System.out.println("sout: testFail2");
        System.err.println("serr: testFail2");
        Assert.fail("This test also fails.");
    }

    @Test
    public void testPass1() {
        System.out.println("sout: testPass1");
        System.err.println("serr: testPass1");
        Assert.assertTrue("However, this test passes.", true);
    }
}
