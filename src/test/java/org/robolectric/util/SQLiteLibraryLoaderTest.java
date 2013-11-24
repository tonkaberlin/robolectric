package org.robolectric.util;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.TestRunners;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(TestRunners.WithDefaults.class)
public class SQLiteLibraryLoaderTest {

  /** Saved system properties. */
  private String savedOs, savedArch;

  /** Saved library name mapper. */
  private SQLiteLibraryLoader.LibraryNameMapper savedMapper;

  @Before
  public void deleteExtractedLibrary() {
    SQLiteLibraryLoader.getNativeLibraryPath().delete();
    SQLiteLibraryLoader.mustReload();
  }

  @Before
  public void saveSystemProperties() {
    savedOs = System.getProperty("os.name");
    savedArch = System.getProperty("os.arch");
    savedMapper = SQLiteLibraryLoader.libraryNameMapper;
  }

  @After
  public void restoreSystemProperties() {
    System.setProperty("os.name", savedOs);
    System.setProperty("os.arch", savedArch);
    SQLiteLibraryLoader.libraryNameMapper = savedMapper;
  }

  @Test
  public void shouldExtractNativeLibrary() {
    File extractedPath = SQLiteLibraryLoader.getNativeLibraryPath();
    assertThat(extractedPath).doesNotExist();
    SQLiteLibraryLoader.load();
    assertThat(extractedPath).exists();
  }

  @Test
  public void shouldNotRewriteExistingLibraryIfThereAreNoChanges() throws Exception{
    SQLiteLibraryLoader.load();
    File extractedPath = SQLiteLibraryLoader.getNativeLibraryPath();
    assertThat(extractedPath).exists();

    final long resetTime = 1234L;
    assertThat(extractedPath.setLastModified(resetTime)).describedAs("Cannot reset modification date").isTrue();
    // actual time may be truncated to seconds
    long time = extractedPath.lastModified();
    assertThat(time).isLessThanOrEqualTo(resetTime);

    SQLiteLibraryLoader.mustReload();
    SQLiteLibraryLoader.load();
    extractedPath = SQLiteLibraryLoader.getNativeLibraryPath();
    assertThat(extractedPath.lastModified()).isEqualTo(time);
  }

  @Test
  public void shouldRewriteExistingLibraryIfThereAreChanges() throws Exception {
    IOUtils.copy(IOUtils.toInputStream("changed"), new FileOutputStream(SQLiteLibraryLoader.getNativeLibraryPath()));
    long firstSize = SQLiteLibraryLoader.getNativeLibraryPath().length();

    SQLiteLibraryLoader.load();
    File extractedPath = SQLiteLibraryLoader.getNativeLibraryPath();
    assertThat(extractedPath).exists();
    assertThat(extractedPath.length()).isGreaterThan(firstSize);
  }

  @Test
  public void shouldFindLibraryForWindowsXPX86() throws IOException {
    SQLiteLibraryLoader.libraryNameMapper = new LibraryMapperTest("", "dll");
    verifyLibraryLoads("Windows XP", "x86");
  }

  @Test
  public void shouldFindLibraryForWindows7X86() throws IOException {
    SQLiteLibraryLoader.libraryNameMapper = new LibraryMapperTest("", "dll");
    verifyLibraryLoads("Windows 7", "x86");
  }

  @Test
  public void shouldFindLibraryForWindowsXPAmd64() throws IOException {
    SQLiteLibraryLoader.libraryNameMapper = new LibraryMapperTest("", "dll");
    verifyLibraryLoads("Windows XP", "amd64");
  }

  @Test
  public void shouldFindLibraryForWindows7Amd64() throws IOException {
    SQLiteLibraryLoader.libraryNameMapper = new LibraryMapperTest("", "dll");
    verifyLibraryLoads("Windows 7", "amd64");
  }

  @Test
  public void shouldFindLibraryForLinuxi386() throws IOException {
    SQLiteLibraryLoader.libraryNameMapper = new LibraryMapperTest("lib", "so");
    verifyLibraryLoads("Some linux version", "i386");
  }

  @Test
  public void shouldFindLibraryForLinuxAmd64() throws IOException {
    SQLiteLibraryLoader.libraryNameMapper = new LibraryMapperTest("lib", "so");
    verifyLibraryLoads("Some linux version", "amd64");
  }

  @Test
  public void shouldFindLibraryForMacWithAnyArch() throws IOException {
    SQLiteLibraryLoader.libraryNameMapper = new LibraryMapperTest("lib", "jnilib");
    verifyLibraryLoads("Mac OS X", "any architecture");
  }

  @Test
  public void shouldFindLibraryForMacWithAnyArchAndDyLibMapping() throws IOException {
    SQLiteLibraryLoader.libraryNameMapper = new LibraryMapperTest("lib", "dylib");
    verifyLibraryLoads("Mac OS X", "any architecture");
  }

  @Test(expected = UnsupportedOperationException.class)
  public void shouldThrowExceptionIfUnknownNameAndArch() throws Exception {
    SQLiteLibraryLoader.libraryNameMapper = new LibraryMapperTest("lib", "so");
    verifyLibraryLoads("ACME Electronic", "FooBar2000");
  }

  private void verifyLibraryLoads(String name, String arch) throws IOException {
    setNameAndArch(name, arch);
    SQLiteLibraryLoader.getLibraryStream().close();
  }

  private static class LibraryMapperTest implements SQLiteLibraryLoader.LibraryNameMapper {
    private final String prefix;
    private final String ext;

    private LibraryMapperTest(String prefix, String ext) {
      this.prefix = prefix;
      this.ext = ext;
    }

    @Override
    public String mapLibraryName(String name) {
      return prefix + name + "." + ext;
    }
  }

  private static void setNameAndArch(String name, String arch) {
    System.setProperty("os.name", name);
    System.setProperty("os.arch", arch);
  }
}
