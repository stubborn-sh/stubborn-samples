package demo;

/**
 * Reads the versions of the Jetty jars that are actually on this module's test classpath,
 * rather than trusting the poms. Shared by every module in this sample.
 */
final class JettyVersions {

    private JettyVersions() {
    }

    /** The Jetty core (org.eclipse.jetty) the application itself runs on. */
    static String core() {
        return org.eclipse.jetty.server.Server.class.getPackage().getImplementationVersion();
    }

    /** The Jakarta EE 10 servlet layer (org.eclipse.jetty.ee10) WireMock's stub server uses. */
    static String ee10Servlet() {
        try {
            // initialize = false: ServletContextHandler's static initialiser is what blows
            // up on a split classpath, and here we only want the jar's version.
            Class<?> handler = Class.forName("org.eclipse.jetty.ee10.servlet.ServletContextHandler", false,
                    JettyVersions.class.getClassLoader());
            return handler.getPackage().getImplementationVersion();
        }
        catch (ClassNotFoundException ex) {
            throw new AssertionError("no unshaded jetty-ee10-servlet on the classpath", ex);
        }
    }
}
