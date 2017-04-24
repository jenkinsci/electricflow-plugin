
// FileHelper.java --
//
// FileHelper.java is part of ElectricCommander.
//
// Copyright (c) 2005-2017 Electric Cloud, Inc.
// All rights reserved.
//

package org.jenkinsci.plugins.electricflowintegration;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.maven.shared.utils.io.DirectoryScanner;

public class FileHelper
{

    //~ Methods ----------------------------------------------------------------

    static void modifyFile(
            String filePath,
            String oldString,
            String newString)
        throws IOException
    {
        StringBuilder  oldContent = new StringBuilder();
        BufferedReader reader     = new BufferedReader(new InputStreamReader(
                    new FileInputStream(filePath), "UTF-8"));

        // Reading all the lines of input text file into oldContent
        String line;

        while ((line = reader.readLine()) != null) {
            oldContent.append(line)
                      .append(System.lineSeparator());
        }

        // Replacing oldString with newString in the oldContent
        String newContent = oldContent.toString().replace(oldString, newString);

        // Rewriting the input text file with newContent
        Writer out = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(filePath), "UTF-8"));

        out.append(newContent);
        out.flush();

        // Closing the resources
        reader.close();
        out.close();
    }
    static List <File> getFilesFromDirectoryWildcard(final String basePath, final String path) {
        String[] splitResult = splitPath(path);

        List <File> result = new ArrayList<>();
        // Now let's locate files
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setIncludes(splitResult);
        scanner.setBasedir(basePath);
        scanner.setCaseSensitive(false);
        scanner.scan();
        String[] files = scanner.getIncludedFiles();
        for (String str : files) {
            result.add(new File(basePath + "/" + str));
        }
        return result;

    }
    static List<File> getFilesFromDirectory(final File folder) {
        List<File> fileList = new ArrayList<>();
        File[]     list     = folder.listFiles();

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
}
