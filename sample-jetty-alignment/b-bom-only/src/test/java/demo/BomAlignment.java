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

    private BomAlignment() {
    }

    /**
     * True when the BOM left Jetty core alone and aligned ee10 with it — Stubborn 0.2.1 and
     * later. False when the BOM pinned the whole of Jetty down to the 12.0.x line, which is
     * what 0.2.0 did.
     */
    static boolean alignedWithBoot() {
        return JettyVersions.core().startsWith("12.1.");
    }
}
