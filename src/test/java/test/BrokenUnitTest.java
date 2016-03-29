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
    public void testFail() {
        Assert.fail();
    }
}
