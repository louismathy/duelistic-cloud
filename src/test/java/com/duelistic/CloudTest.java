package com.duelistic;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Smoke test placeholder for the cloud module.
 */
public class CloudTest
    extends TestCase
{
    /**
     * Creates the test case.
     *
     * @param testName name of the test case
     */
    public CloudTest(String testName )
    {
        super( testName );
    }

    /**
     * Builds the test suite for this class.
     *
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( CloudTest.class );
    }

    /**
     * Verifies the test harness runs.
     */
    public void testApp()
    {
        assertTrue( true );
    }
}
