/*
 * The MIT License
 *
 * Copyright (c) 2013 IKEDA Yasuyuki
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package jp.ikedam.jenkins.plugins.extensible_choice_parameter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jp.ikedam.jenkins.plugins.extensible_choice_parameter.FilenameChoiceListProvider.EmptyChoiceType;
import jp.ikedam.jenkins.plugins.extensible_choice_parameter.FilenameChoiceListProvider.ScanType;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

/**
 * Tests for FilenameChoiceListProvider, not concerned with Jenkins.
 */
class FilenameChoiceListProviderSimpleTest {

    private File createTempDir() throws IOException {
        return Files.createTempDirectory("test").toFile();
    }

    @Test
    void testFilenameChoiceListProvider() {
        // simple value
        {
            String baseDirPath = "baseDirPath";
            String includePattern = "includePattern";
            String excludePattern = "excludePattern";
            ScanType scanType = ScanType.File;
            boolean reverseOrder = false;
            FilenameChoiceListProvider target =
                    new FilenameChoiceListProvider(baseDirPath, includePattern, excludePattern, scanType, reverseOrder);
            assertEquals(baseDirPath, target.getBaseDirPath(), "simple value for baseDirPath");
            assertEquals(includePattern, target.getIncludePattern(), "simple value for includePattern");
            assertEquals(excludePattern, target.getExcludePattern(), "simple value for excludePattern");
            assertEquals(scanType, target.getScanType(), "scanType must be reserved");
            assertEquals(reverseOrder, target.isReverseOrder());
        }

        // null
        {
            String baseDirPath = null;
            String includePattern = null;
            String excludePattern = null;
            ScanType scanType = null;
            boolean reverseOrder = false;
            FilenameChoiceListProvider target =
                    new FilenameChoiceListProvider(baseDirPath, includePattern, excludePattern, scanType, reverseOrder);
            assertEquals(baseDirPath, target.getBaseDirPath(), "null for baseDirPath");
            assertEquals(includePattern, target.getIncludePattern(), "null for includePattern");
            assertEquals(excludePattern, target.getExcludePattern(), "null for excludePattern");
            assertEquals(scanType, target.getScanType(), "scanType must be reserved");
            assertEquals(reverseOrder, target.isReverseOrder());
        }

        // empty
        {
            String baseDirPath = "";
            String includePattern = "";
            String excludePattern = "";
            ScanType scanType = ScanType.Directory;
            boolean reverseOrder = true;
            FilenameChoiceListProvider target =
                    new FilenameChoiceListProvider(baseDirPath, includePattern, excludePattern, scanType, reverseOrder);
            assertEquals(baseDirPath, target.getBaseDirPath(), "empty for baseDirPath");
            assertEquals(includePattern, target.getIncludePattern(), "empty for includePattern");
            assertEquals(excludePattern, target.getExcludePattern(), "empty for excludePattern");
            assertEquals(scanType, target.getScanType(), "scanType must be reserved");
            assertEquals(reverseOrder, target.isReverseOrder());
        }

        // blank
        {
            String baseDirPath = "";
            String includePattern = "";
            String excludePattern = "";
            ScanType scanType = ScanType.FileAndDirectory;
            boolean reverseOrder = true;
            FilenameChoiceListProvider target = new FilenameChoiceListProvider(
                    "  " + baseDirPath + "  ",
                    "\t\t" + includePattern + " ",
                    excludePattern + " ",
                    scanType,
                    reverseOrder);
            assertEquals(baseDirPath, target.getBaseDirPath(), "blank for baseDirPath");
            assertEquals(includePattern, target.getIncludePattern(), "blank for includePattern");
            assertEquals(excludePattern, target.getExcludePattern(), "blank for excludePattern");
            assertEquals(scanType, target.getScanType(), "scanType must be reserved");
            assertEquals(reverseOrder, target.isReverseOrder());
        }

        // enclosed with blank
        {
            String baseDirPath = "baseDirPath";
            String includePattern = "includePattern";
            String excludePattern = "excludePattern";
            ScanType scanType = ScanType.File;
            boolean reverseOrder = true;
            FilenameChoiceListProvider target = new FilenameChoiceListProvider(
                    "  " + baseDirPath + "  ",
                    "\t\t" + includePattern + " ",
                    excludePattern + " ",
                    scanType,
                    reverseOrder);
            assertEquals(baseDirPath, target.getBaseDirPath(), "baseDirPath must be trimmed");
            assertEquals(includePattern, target.getIncludePattern(), "includePattern must be trimmed");
            assertEquals(excludePattern, target.getExcludePattern(), "excludePattern must be trimmed");
        }
    }

    private static class FilenameChoiceListProviderForTest extends FilenameChoiceListProvider {
        @Serial
        private static final long serialVersionUID = 5830671030985340194L;

        public FilenameChoiceListProviderForTest(
                String baseDirPath,
                String includePattern,
                String excludePattern,
                ScanType scanType,
                boolean reverseOrder,
                EmptyChoiceType emptyChoideType) {
            super(baseDirPath, includePattern, excludePattern, scanType, reverseOrder, emptyChoideType);
        }

        public FilenameChoiceListProviderForTest(
                String baseDirPath,
                String includePattern,
                String excludePattern,
                ScanType scanType,
                boolean reverseOrder) {
            this(baseDirPath, includePattern, excludePattern, scanType, reverseOrder, null);
        }

        public static List<String> getFileList(
                File baseDir, String includePattern, String excludePattern, ScanType scanType, boolean reverseOrder) {
            return FilenameChoiceListProvider.getFileList(
                    baseDir, includePattern, excludePattern, scanType, reverseOrder);
        }
    }

    private static void assertFileListEquals(String message, List<String> expected, List<String> fileList) {
        assertEquals(expected.size(), fileList.size(), message);
        for (String file : fileList) {
            if (file.contains("\\")) {
                // fix windows path to unix path
                file = file.replace('\\', '/');
            }
            assertTrue(expected.contains(file), String.format("%s: must contains %s", message, file));
        }
    }

    @Test
    void testGetFileList() throws IOException {
        File tempDir = createTempDir();
        File emptyDir = createTempDir();
        try {
            // prepare files to test
            // tempdir
            //    test1.txt
            //    test2.dat
            //    dir1/
            //       test3.txt
            //    dir2/
            //       dir3/
            //          test4.dat
            {
                FileUtils.writeStringToFile(new File(tempDir, "test1.txt"), "test", StandardCharsets.UTF_8);
                FileUtils.writeStringToFile(new File(tempDir, "test2.dat"), "test", StandardCharsets.UTF_8);
                new File(tempDir, "dir1").mkdir();
                FileUtils.writeStringToFile(new File(tempDir, "dir1/test3.txt"), "test", StandardCharsets.UTF_8);
                new File(tempDir, "dir2").mkdir();
                new File(tempDir, "dir2/dir3").mkdir();
                FileUtils.writeStringToFile(new File(tempDir, "dir2/dir3/test4.dat"), "test", StandardCharsets.UTF_8);
            }

            // List all files and directories.
            {
                List<String> expected = Arrays.asList(
                        "test1.txt", "test2.dat", "dir1", "dir1/test3.txt", "dir2", "dir2/dir3", "dir2/dir3/test4.dat");
                List<String> fileList = FilenameChoiceListProviderForTest.getFileList(
                        tempDir, "**/*", "", ScanType.FileAndDirectory, false);
                assertFileListEquals("All files and directories", expected, fileList);
            }

            // List all files and directories reversed.
            {
                List<String> expected = Arrays.asList(
                        "dir2/dir3/test4.dat", "dir2/dir3", "dir2", "dir1/test3.txt", "dir1", "test2.dat", "test1.txt");
                List<String> fileList = FilenameChoiceListProviderForTest.getFileList(
                        tempDir, "**/*", "", ScanType.FileAndDirectory, true);
                assertFileListEquals("All files and directories", expected, fileList);
            }

            // List all files.
            {
                List<String> expected =
                        Arrays.asList("test1.txt", "test2.dat", "dir1/test3.txt", "dir2/dir3/test4.dat");
                List<String> fileList =
                        FilenameChoiceListProviderForTest.getFileList(tempDir, "**/*", "", ScanType.File, false);
                assertFileListEquals("All files", expected, fileList);
            }

            // List all files reversed.
            {
                List<String> expected =
                        Arrays.asList("dir2/dir3/test4.dat", "dir1/test3.txt", "test2.dat", "test1.txt");
                List<String> fileList =
                        FilenameChoiceListProviderForTest.getFileList(tempDir, "**/*", "", ScanType.File, true);
                assertFileListEquals("All files", expected, fileList);
            }

            // List all directories.
            {
                List<String> expected = Arrays.asList("dir1", "dir2", "dir2/dir3");
                List<String> fileList =
                        FilenameChoiceListProviderForTest.getFileList(tempDir, "**/*", "", ScanType.Directory, false);
                assertFileListEquals("All files and directories", expected, fileList);
            }

            // List all directories reversed.
            {
                List<String> expected = Arrays.asList("dir2/dir3", "dir2", "dir1");
                List<String> fileList =
                        FilenameChoiceListProviderForTest.getFileList(tempDir, "**/*", "", ScanType.Directory, true);
                assertFileListEquals("All files and directories", expected, fileList);
            }

            // filter with included pattern.
            {
                List<String> expected = Arrays.asList("test1.txt", "dir1/test3.txt");
                List<String> fileList = FilenameChoiceListProviderForTest.getFileList(
                        tempDir, "**/*.txt", "", ScanType.FileAndDirectory, false);
                assertFileListEquals("*.txt", expected, fileList);
            }

            // filter with multiple included pattern.
            {
                List<String> expected = Arrays.asList("test1.txt", "dir1/test3.txt", "dir2/dir3/test4.dat");
                List<String> fileList = FilenameChoiceListProviderForTest.getFileList(
                        tempDir, "**/*.txt  , ,, **/test4.dat", "", ScanType.FileAndDirectory, false);
                assertFileListEquals("*.txt,test4.dat", expected, fileList);
            }

            // filter with excluded pattern.
            {
                List<String> expected = Arrays.asList("test1.txt", "dir1", "dir1/test3.txt", "dir2", "dir2/dir3");
                List<String> fileList = FilenameChoiceListProviderForTest.getFileList(
                        tempDir, "**/*", "**/*.dat", ScanType.FileAndDirectory, false);
                assertFileListEquals("exclude *.dat", expected, fileList);
            }

            // filter with multiple excluded pattern.
            {
                List<String> expected = Arrays.asList("test1.txt", "dir1/test3.txt", "dir2", "dir2/dir3");
                List<String> fileList = FilenameChoiceListProviderForTest.getFileList(
                        tempDir, "**/*", "**/*.dat,, , **/dir1", ScanType.FileAndDirectory, false);
                assertFileListEquals("exclude *.dat, dir1", expected, fileList);
            }

            // all file excluded.
            {
                List<String> expected = new ArrayList<>(0);
                List<String> fileList = FilenameChoiceListProviderForTest.getFileList(
                        tempDir, "**/*", "**/*", ScanType.FileAndDirectory, false);
                assertFileListEquals("excluded all files", expected, fileList);
            }

            // no file included.
            {
                List<String> expected = new ArrayList<>(0);
                List<String> fileList = FilenameChoiceListProviderForTest.getFileList(
                        tempDir, "", "", ScanType.FileAndDirectory, false);
                assertFileListEquals("no file included", expected, fileList);
            }

            // no file.
            {
                List<String> expected = new ArrayList<>(0);
                List<String> fileList = FilenameChoiceListProviderForTest.getFileList(
                        emptyDir, "**/*", "", ScanType.FileAndDirectory, false);
                assertFileListEquals("no file", expected, fileList);
            }

            // non-exist directory.
            {
                List<String> expected = new ArrayList<>(0);
                List<String> fileList = FilenameChoiceListProviderForTest.getFileList(
                        new File(emptyDir, "test"), "**/*", "", ScanType.FileAndDirectory, false);
                assertFileListEquals("non-exist directory", expected, fileList);
            }

            // not directory.
            {
                List<String> expected = new ArrayList<>(0);
                List<String> fileList = FilenameChoiceListProviderForTest.getFileList(
                        new File(tempDir, "test1.txt"), "**/*", "", ScanType.FileAndDirectory, false);
                assertFileListEquals("not directory", expected, fileList);
            }

            // null for directory.
            {
                List<String> expected = new ArrayList<>(0);
                List<String> fileList = FilenameChoiceListProviderForTest.getFileList(
                        null, "**/*", "", ScanType.FileAndDirectory, false);
                assertFileListEquals("null for directory", expected, fileList);
            }

            // null for include pattern.
            {
                List<String> expected = new ArrayList<>(0);
                List<String> fileList = FilenameChoiceListProviderForTest.getFileList(
                        tempDir, null, "", ScanType.FileAndDirectory, false);
                assertFileListEquals("null for include pattern", expected, fileList);
            }

            // null for exclude pattern.
            {
                List<String> expected = Arrays.asList(
                        "test1.txt", "test2.dat", "dir1", "dir1/test3.txt", "dir2", "dir2/dir3", "dir2/dir3/test4.dat");
                List<String> fileList = FilenameChoiceListProviderForTest.getFileList(
                        tempDir, "**/*", null, ScanType.FileAndDirectory, false);
                assertFileListEquals("null for exclude pattern", expected, fileList);
            }

            // null for scanType.
            // treat as file.
            {
                List<String> expected =
                        Arrays.asList("test1.txt", "test2.dat", "dir1/test3.txt", "dir2/dir3/test4.dat");
                List<String> fileList = FilenameChoiceListProviderForTest.getFileList(tempDir, "**/*", "", null, false);
                assertFileListEquals("null for scanType", expected, fileList);
            }

            // No empty choice.
            {
                List<String> expected = Arrays.asList(
                        "test1.txt", "test2.dat", "dir1", "dir1/test3.txt", "dir2", "dir2/dir3", "dir2/dir3/test4.dat");
                List<String> fileList = FilenameChoiceListProviderForTest.getFileList(
                        tempDir, "**/*", "", ScanType.FileAndDirectory, false, EmptyChoiceType.None);
                assertFileListEquals("No empty choice", expected, fileList);
            }

            // Empty choice at the top.
            {
                List<String> expected = Arrays.asList(
                        "",
                        "test1.txt",
                        "test2.dat",
                        "dir1",
                        "dir1/test3.txt",
                        "dir2",
                        "dir2/dir3",
                        "dir2/dir3/test4.dat");
                List<String> fileList = FilenameChoiceListProviderForTest.getFileList(
                        tempDir, "**/*", "", ScanType.FileAndDirectory, false, EmptyChoiceType.AtTop);
                assertFileListEquals("Empty choice at the top", expected, fileList);
            }

            // Empty choice at the end.
            {
                List<String> expected = Arrays.asList(
                        "test1.txt",
                        "test2.dat",
                        "dir1",
                        "dir1/test3.txt",
                        "dir2",
                        "dir2/dir3",
                        "dir2/dir3/test4.dat",
                        "");
                List<String> fileList = FilenameChoiceListProviderForTest.getFileList(
                        tempDir, "**/*", "", ScanType.FileAndDirectory, false, EmptyChoiceType.AtEnd);
                assertFileListEquals("Empty choice at the end", expected, fileList);
            }

            // Empty choice at the top and reversed
            {
                List<String> expected =
                        Arrays.asList("", "dir2/dir3/test4.dat", "dir1/test3.txt", "test2.dat", "test1.txt");
                List<String> fileList = FilenameChoiceListProviderForTest.getFileList(
                        tempDir, "**/*", "", ScanType.File, true, EmptyChoiceType.AtTop);
                assertFileListEquals("Empty choice at the top and reversed", expected, fileList);
            }

            // Empty choice at the end and reversed.
            {
                List<String> expected = Arrays.asList("dir2/dir3", "dir2", "dir1", "");
                List<String> fileList = FilenameChoiceListProviderForTest.getFileList(
                        tempDir, "**/*", "", ScanType.Directory, true, EmptyChoiceType.AtEnd);
                assertFileListEquals("Empty choice at the end and reversed", expected, fileList);
            }
        } finally {
            FileUtils.deleteDirectory(tempDir);
            FileUtils.deleteDirectory(emptyDir);
        }
    }
}
