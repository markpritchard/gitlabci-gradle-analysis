package test;

import org.junit.Assert;
import org.junit.Test;

public class PassingUnitTest {

    @Test
    public void testPass1() {
        Assert.assertTrue("Test 1 passes.", true);
    }

    @Test
    public void testPass2() {
        Assert.assertTrue("Test 2 also passes.", true);
    }

}
