package demo;

/**
 * Which behaviour this module should be asserting, decided from the jars that Stubborn's
 * BOM actually put on the classpath rather than from a version string.
 *
 * <p>The point of this module is that one unchanged pom behaves differently depending on
 * which Stubborn BOM it imports, and that the samples keep verifying whichever one is
 * pinned. scripts/set-samples-version.sh rewrites stubborn-contract.version across every
 * sample when a release lands, and the bump only merges if CI is green — so a test that
 * hardcoded "this is broken" would block the 0.2.1 bump instead of demonstrating it.
 */
final class BomAlignment {

    /**
     * The Jetty that Spring Boot manages, handed in by Surefire from the parent's
     * ${jetty.version}. Compared against rather than hardcoded, so this keeps telling the
     * truth when Boot moves to a Jetty line that does not exist yet.
     */
    private static final String BOOT_JETTY_VERSION = System.getProperty("boot.jetty.version", "");

    private BomAlignment() {
    }

    /**
     * True when the BOM left Jetty core alone, so the application runs on exactly the Jetty
     * Boot chose — Stubborn 0.2.1 and later. False when the BOM overrode it, which is what
     * 0.2.0 did by pinning the whole of Jetty down to the 12.0.x line.
     */
    static boolean alignedWithBoot() {
        if (BOOT_JETTY_VERSION.isEmpty()) {
            throw new AssertionError("boot.jetty.version was not passed to the tests; "
                    + "see systemPropertyVariables in this module's pom");
        }
        return BOOT_JETTY_VERSION.equals(JettyVersions.core());
    }
}
