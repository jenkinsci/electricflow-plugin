// FileHelper.java --
//
// FileHelper.java is part of ElectricCommander.
//
// Copyright (c) 2005-2017 Electric Cloud, Inc.
// All rights reserved.
//

package org.jenkinsci.plugins.electricflow;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.maven.shared.utils.io.DirectoryScanner;

public class FileHelper {

  // ~ Methods ----------------------------------------------------------------

  public static String buildPath(String... path) {
    StringBuilder result = new StringBuilder();

    for (String s : path) {

      if (s.equals("/")) {
        s = File.separator;
      }

      result.append(s);
    }

    return result.toString();
  }

  public static String cutTopLevelDir(String path) {
    return cutTopLevelDir(splitFileSystemPath(path));
  }

  public static String cutTopLevelDir(String[] path) {
    StringBuilder result = new StringBuilder();
    int len = path.length;

    len -= 1;

    for (int i = 1; i < len; i++) {
      result.append(path[i]).append(File.separator);
    }

    result.append(path[len]);

    return result.toString();
  }

  static void modifyFile(String filePath, String oldString, String newString) throws IOException {
    StringBuilder oldContent = new StringBuilder();
    BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8));

    // Reading all the lines of input text file into oldContent
    String line;

    while ((line = reader.readLine()) != null) {
      oldContent.append(line).append(System.lineSeparator());
    }

    // Replacing oldString with newString in the oldContent
    String newContent = oldContent.toString().replace(oldString, newString);

    // Rewriting the input text file with newContent
    Writer out =
        new BufferedWriter(
            new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8));

    out.append(newContent);
    out.flush();

    // Closing the resources
    reader.close();
    out.close();
  }

  static String[] splitFileSystemPath(String path) {
    return path.split(Pattern.quote(File.separator));
  }

  static String[] splitPath(String path) {
    return splitPath(",", path);
  }

  static String[] splitPath(String separator, String path) {
    String[] list = path.split(separator);

    for (int i = 0; i < list.length; i++) {
      list[i] = list[i].trim();
    }

    return list;
  }

  static List<File> getFilesFromDirectory(final File folder) {
    List<File> fileList = new ArrayList<>();
    File[] list = folder.listFiles();

    if (list == null) {
      return fileList;
    }

    for (final File fileEntry : list) {

      if (!fileEntry.isDirectory()) {
        fileList.add(fileEntry);
      }
    }

    return fileList;
  }

  static List<File> getFilesFromDirectoryWildcard(
      Run build, TaskListener listener, final FilePath basePath, final String path)
      throws IOException, InterruptedException {
    return getFilesFromDirectoryWildcard(build, listener, basePath, path, false, false);
  }

  static FilePath getPublishArtifactWorkspaceOnMaster(Run run) {
    return new FilePath(new File(run.getRootDir(), "publish-artifact"));
  }

  static List<File> getFilesFromDirectoryWildcard(
      Run build,
      TaskListener listener,
      FilePath basePathInitial,
      final String path,
      boolean fullPath,
      boolean copyToMasterBuildDir)
      throws IOException, InterruptedException {
    PrintStream logger = listener.getLogger();
    String[] splitResult = splitPath(path);
    List<File> result = new ArrayList<>();
    DirectoryScanner scanner = new DirectoryScanner();

    String basePathActual = basePathInitial.getRemote();

    if (copyToMasterBuildDir) {
      FilePath basePathOnMaster = getPublishArtifactWorkspaceOnMaster(build);
      logger.println(
          "Copying files from: "
              + basePathInitial.toURI()
              + " to reports directory: "
              + basePathOnMaster.toURI());
      basePathInitial.copyRecursiveTo("**", "", basePathOnMaster);
      basePathActual = basePathOnMaster.getRemote();
    }

    scanner.setBasedir(basePathActual);

    // Now let's locate files

    scanner.setIncludes(splitResult);
    scanner.setCaseSensitive(false);
    scanner.scan();

    String[] files = scanner.getIncludedFiles();

    for (String str : files) {

      if (fullPath) {
        result.add(new File(buildPath(basePathActual, "/", str)));
      } else {
        result.add(new File(str));
      }
    }

    if (result.isEmpty()) {
      throw new InterruptedException(
          "Upload result:  No files were found in path \""
              + basePathActual
              + File.separator
              + path
              + "\".");
    }

    return result;
  }

  public static boolean isTopLevelDirSame(List<File> files) {
    String buffer = "";
    boolean sameRoot = false;

    for (File f : files) {
      String[] newFilePathCut = splitFileSystemPath(f.getPath());

      if (buffer.isEmpty()) {
        sameRoot = true;
        buffer = newFilePathCut[0];
      } else {

        if (!buffer.equals(newFilePathCut[0])) {
          sameRoot = false;
        }
      }
    }

    return sameRoot;
  }

  public static void removeTempDirectory(Run<?, ?> run) throws IOException {
    File directory = new File(run.getRootDir(), "publish-artifact");
    deleteDirectory(directory);
  }

  /**
   * Deletes directory recursively
   *
   * @param dir {@link File} Directory to delete
   */
  private static void deleteDirectory(File dir) {
    File[] files = dir.listFiles();
    if (files != null) {
      for (final File file : files) {
        deleteDirectory(file);
      }
    }
    dir.delete();
  }
}
