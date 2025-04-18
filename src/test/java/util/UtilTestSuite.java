package util;

import org.junit.platform.suite.api.ExcludeClassNamePatterns;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

/**
 * Test suite for running all utility class tests.
 * ArtistWhiteBoxTest is excluded as specified.
 */
@Suite
@SelectPackages("util")
@ExcludeClassNamePatterns("ArtistWhiteBoxTest")
public class UtilTestSuite {
    // This class serves as a suite declaration and doesn't need any implementation.
} 