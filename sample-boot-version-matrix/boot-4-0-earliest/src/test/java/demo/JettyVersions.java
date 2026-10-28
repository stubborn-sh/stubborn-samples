package demo;

/**
 * The Jetty jars actually on this module's test classpath, read at runtime rather than
 * taken from the poms.
 */
final class JettyVersions {

    /** Boot's own Jetty, handed in by Surefire from the parent's ${jetty.version}. */
    static final String BOOT_JETTY = System.getProperty("boot.jetty.version", "");

    /** The Spring Boot version this module pins, for readable failure messages. */
    static final String BOOT = System.getProperty("boot.version", "");

    private JettyVersions() {
    }

    /** The Jetty core (org.eclipse.jetty) the application itself runs on. */
    static String core() {
        return org.eclipse.jetty.server.Server.class.getPackage().getImplementationVersion();
    }

    /** The Jakarta EE 10 servlet layer (org.eclipse.jetty.ee10) the stub server uses. */
    static String ee10Servlet() {
        return org.eclipse.jetty.ee10.servlet.ServletContextHandler.class.getPackage()
                .getImplementationVersion();
    }

    /**
     * The minor line of a Jetty version — "12.1.4" and "12.1.14" are both "12.1". gh-142
     * was a mismatch at this level; a difference in the trailing patch is what these
     * modules are here to vet.
     */
    static String minorLine(String version) {
        String[] parts = version.split("\\.");
        return parts.length >= 2 ? parts[0] + "." + parts[1] : version;
    }
}
