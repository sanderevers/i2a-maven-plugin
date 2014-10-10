package nl.topicus.plugins.maven.i2a;

import java.io.File;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;

public class MyMojoTest
    extends AbstractMojoTestCase
{
//    /** {@inheritDoc} */
//    protected void setUp()
//        throws Exception
//    {
//        // required
//        super.setUp();
//
//        ...
//    }
//
//    /** {@inheritDoc} */
//    protected void tearDown()
//        throws Exception
//    {
//        // required
//        super.tearDown();
//
//        ...
//    }

    /**
     * @throws Exception if any
     */
    public void testSomething()
        throws Exception
    {
        File pom = getTestFile( "src/test/resources/unit/pom.xml" );
        assertNotNull( pom );
        assertTrue( pom.exists() );

        I2A myMojo = (I2A) lookupMojo( "translate", pom );
        assertNotNull( myMojo );
        myMojo.execute();
    }
}
