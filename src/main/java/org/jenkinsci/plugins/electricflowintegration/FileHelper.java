
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
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class FileHelper
{

    //~ Methods ----------------------------------------------------------------

    static void modifyFile(
            String filePath,
            String oldString,
            String newString)
        throws IOException
    {
        StringBuilder     oldContent = new StringBuilder();
        FileWriter writer;

        // FileReader     fileReader       = new FileReader(fileToBeModified);
        // BufferedReader reader           = new BufferedReader(fileReader);
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(filePath), "UTF-8"));

        // Reading all the lines of input text file into oldContent
        String line;

        while ((line = reader.readLine()) != null) {
            oldContent.append(line).append(System.lineSeparator());
        }

        // Replacing oldString with newString in the oldContent
        String newContent = oldContent.toString().replaceAll(oldString, newString);

        // Rewriting the input text file with newContent

        Writer out = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(filePath), "UTF-8"));
        out.append(newContent);
        out.flush();

        // Closing the resources
        reader.close();
        out.close();

    }
    static List <File> getFilesFromDirectory(final File folder) {
        List<File> fileList = new ArrayList<>();
        File[] list = folder.listFiles();

        if (list == null) {
            return fileList;
        }
        
        //for (final File fileEntry : folder.listFiles()) {
        for (final File fileEntry : list) {
            if (fileEntry.isDirectory()) {
                // listFilesForFolder(fileEntry);
                continue;
            }
            else {
                // fileList.add(fileEntry.getAbsolutePath());
                fileList.add(fileEntry);
            }
        }
        return fileList;
    }
}
