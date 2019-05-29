
// ElectricFlowPublishApplication.java --
//
// ElectricFlowPublishApplication.java is part of ElectricCommander.
//
// Copyright (c) 2005-2017 Electric Cloud, Inc.
// All rights reserved.
//

package org.jenkinsci.plugins.electricflow;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jenkinsci.plugins.electricflow.ui.HtmlUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import net.sf.json.JSONObject;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;

import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;

import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

public class ElectricFlowPublishApplication
    extends Publisher
{

    //~ Static fields/initializers ---------------------------------------------

    private static final Log   log                   = LogFactory.getLog(
            ElectricFlowPublishApplication.class);
    public static final String deploymentPackageName = "deployment_package.zip";
    private static List<File>  zipFiles              = new ArrayList<>();
    private static boolean     isCutTopLevelDir;

    //~ Instance fields --------------------------------------------------------

    private final String MANIFEST_NAME = "manifest.json";
    private final String configuration;
    private String       filePath;

    //~ Constructors -----------------------------------------------------------

    @DataBoundConstructor public ElectricFlowPublishApplication(
            String configuration,
            String artifactName,
            String filePath)
    {
        this.configuration = configuration;
        this.filePath      = filePath;
    }

    //~ Methods ----------------------------------------------------------------

    @Override public boolean perform(
            AbstractBuild build,
            Launcher      launcher,
            BuildListener listener)
        throws InterruptedException
    {
        PrintStream logger    = listener.getLogger();
        FilePath    workspace = build.getWorkspace();

        if (workspace == null) {
            logger.println("WARNING: Workspace should not be null.");
            log.warn("Workspace should not be null");

            return false;
        }

        String  workspaceDir = workspace.getRemote();
        Integer buildNumber  = build.getNumber();

        // do replace
        String newFilePath;

        try {
            EnvReplacer env = new EnvReplacer(build, listener);

            newFilePath = env.expandEnv(filePath);
        }
        catch (IOException | InterruptedException e) {
            logger.println("Unexpected error during expand \"%s\"" + e);
            log.warn(e);
            newFilePath = filePath;
        }

        // artifact version
        String artifactVersion = buildNumber.toString();

        try {
            makeApplicationArchive(build, listener, workspaceDir, newFilePath);
        }
        catch (IOException | InterruptedException e) {
            logger.println("Warning: Cannot create archive: " + e.getMessage());
            log.warn("Can't create archive: " + e.getMessage(), e);

            return false;
        }

        String artifactGroup = "org.ec";
        String artifactKey   = getCurrentTimeStamp();

        if (log.isDebugEnabled()) {
            log.debug("ArtifactKey" + artifactKey);
        }

        String artifactName   = artifactGroup + ":" + artifactKey;
        String deployResponse;

        try {
            ElectricFlowClient efClient = new ElectricFlowClient(configuration,
                    workspaceDir);

            efClient.uploadArtifact(build, listener, "default", artifactName,
                artifactVersion,
                ElectricFlowPublishApplication.deploymentPackageName, true);
            deployResponse = efClient.deployApplicationPackage(artifactGroup,
                    artifactKey, artifactVersion,
                    ElectricFlowPublishApplication.deploymentPackageName);

            String            summaryHtml = getSummaryHtml(efClient,
                    workspaceDir, logger);
            SummaryTextAction action      = new SummaryTextAction(build,
                    summaryHtml);

            build.addAction(action);
            build.save();

            if (log.isDebugEnabled()) {
                log.debug("DeployApp response: " + deployResponse);
            }
        }
        catch (Exception e) {
            logger.println(
                "Warning: Error occurred during application creation: "
                    + e.getMessage());
            log.warn("Error occurred during application creation: "
                    + e.getMessage(), e);

            return false;
        }

        return true;
    }

    /**
     * We'll use this from the {@code config.jelly}.
     *
     * @return  we'll use this from the {@code config.jelly}.
     */
    public String getConfiguration()
    {
        return configuration;
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override public DescriptorImpl getDescriptor()
    {
        return (DescriptorImpl) super.getDescriptor();
    }

    public String getFilePath()
    {
        return filePath;
    }

    @Override public BuildStepMonitor getRequiredMonitorService()
    {
        return BuildStepMonitor.NONE;
    }

    private String getSummaryHtml(
            ElectricFlowClient efClient,
            String             workspaceDir,
            PrintStream        logger)
    {
        String url         = efClient.getElectricFlowUrl()
                + "/flow/#applications";
        String summaryText =
            "<h3>ElectricFlow Create/Deploy Application from Deployment Package</h3>"
                + "<table cellspacing=\"2\" cellpadding=\"4\"> \n"
                + "  <tr>\n"
                + "    <td>Application URL:</td>\n"
                + "    <td><a href='" + HtmlUtils.encodeForHtml(url) + "'>" + HtmlUtils.encodeForHtml(url) + "</a></td>   \n"
                + "  </tr>\n";

        if (!zipFiles.isEmpty()) {
            StringBuilder strBuilder = new StringBuilder(summaryText);

            strBuilder.append("  <tr>\n"
                    + "    <td><b>Deployment Package Details:</b></td>\n"
                    + "    <td></td>    \n"
                    + "  </tr>\n");

            String jsonContent = "";

            for (File file : zipFiles) {
                String fileName = file.getPath();

                if (isCutTopLevelDir) {
                    fileName = FileHelper.cutTopLevelDir(file.getPath());
                }

                if (fileName.endsWith(MANIFEST_NAME)) {
                    String manifestPath = FileHelper.buildPath(workspaceDir,
                            "/", fileName);

                    try {
                        byte[] encoded = Files.readAllBytes(Paths.get(
                                    manifestPath));

                        jsonContent = new String(encoded, "UTF-8");
                    }
                    catch (IOException e) {
                        logger.println(
                            "Warning: Error occurred during read manifest file. "
                                + e.getMessage());
                        log.warn(e.getMessage(), e);
                    }

                    continue;
                }

                strBuilder.append("  <tr>\n"
                                  + "    <td>&nbsp;&nbsp;&nbsp;&nbsp;")
                          .append(HtmlUtils.encodeForHtml(fileName))
                          .append("</td>\n"
                              + "    <td>")
                          .append("</td>    \n"
                              + "  </tr>\n");
            }

            if (!jsonContent.isEmpty()) {
                strBuilder.append("  <tr>\n"
                                  + "    <td>&nbsp;&nbsp;&nbsp;&nbsp;")
                          .append(HtmlUtils.encodeForHtml(MANIFEST_NAME))
                          .append("</td>\n"
                              + "    <td>")
                          .append("<pre>").append(HtmlUtils.encodeForHtml(jsonContent)).append("</pre>")
                          .append("</td>    \n"
                              + "  </tr>\n");
            }

            summaryText = strBuilder.toString();
        }

        summaryText = summaryText + "</table>";

        return summaryText;
    }

    //~ Methods ----------------------------------------------------------------

    public static File createZipArchive(
            String   basePath,
            String   archiveName,
            String[] files)
        throws IOException
    {
        List<File> fileList = new ArrayList<>();

        for (String file : files) {
            File f = new File(file);

            fileList.add(f);
        }

        return createZipArchive(basePath, archiveName, fileList);
    }

    public static File createZipArchive(
            String     basePath,
            String     archiveName,
            List<File> files)
        throws IOException
    {
        return createZipArchive(basePath, archiveName, files, false);
    }

    public static File createZipArchive(
            String     basePath,
            String     archiveName,
            List<File> files,
            boolean    cutTopLevelDir)
        throws IOException
    {
        isCutTopLevelDir = cutTopLevelDir;

        File archive = new File(archiveName);

        try(ZipOutputStream out = new ZipOutputStream(
                        new FileOutputStream(archive))) {

            if (cutTopLevelDir) {
                cutTopLevelDir = FileHelper.isTopLevelDirSame(files);
            }

            for (File row : files) {

                try(FileInputStream in = new FileInputStream(
                                FileHelper.buildPath(basePath, "/",
                                    row.getPath()))) {
                    String filePathToAdd = row.getPath();

                    if (cutTopLevelDir) {
                        filePathToAdd = FileHelper.cutTopLevelDir(
                                filePathToAdd);
                    }

                    out.putNextEntry(new ZipEntry(filePathToAdd));

                    int    len;
                    byte[] buf = new byte[1024];

                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }

                    // Complete the entry
                    out.closeEntry();
                }
                catch (IOException e) {
                    throw new IOException("Unable to compress zip file: "
                            + basePath, e);
                }
            }
        }
        catch (IOException e) {
            throw new IOException(e);
        }

        return archive;
    }

    public static File createZipArchive(
            AbstractBuild build,
            BuildListener listener,
            String        basePath,
            String        archiveName,
            String        path)
        throws IOException, InterruptedException
    {
        String fullPath = FileHelper.buildPath(basePath, "/", path);
        File   f        = new File(fullPath);

        if (f.exists() && f.isDirectory()) {
            List<File> fileList = FileHelper.getFilesFromDirectoryWildcard(
                    build, listener, fullPath, "**");

            setZipFiles(fileList);

            return createZipArchive(fullPath, archiveName, fileList);
        }

        List<File> filesToArchive = FileHelper.getFilesFromDirectoryWildcard(
                build, listener, basePath, path);

        setZipFiles(filesToArchive);

        return createZipArchive(basePath, archiveName, filesToArchive, true);
    }

    // This methods
    public static File makeApplicationArchive(
            AbstractBuild build,
            BuildListener listener,
            String        workspaceDir,
            String        filePath)
        throws IOException, InterruptedException
    {

        // in this method manifest is already tuned, so all we need is just to
        // package archive.
        String archivePath = FileHelper.buildPath(workspaceDir, "/",
                ElectricFlowPublishApplication.deploymentPackageName);

        return createZipArchive(build, listener, workspaceDir, archivePath,
            filePath);
    }

    public static String getCurrentTimeStamp()
    {
        String dateFormat = "yyyy-MM-dd-HH-mm-ss.S";

        return new SimpleDateFormat(dateFormat).format(new Date());
    }

    private static void setZipFiles(List<File> fileList)
    {
        zipFiles.clear();
        zipFiles.addAll(fileList);
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * Descriptor for {@link BuildStepDescriptor}. Used as a singleton. The
     * class is marked as public so that it can be accessed from views.
     *
     * <p>See * .jelly for the actual HTML fragment for the configuration
     * screen.</p>
     */
    @Extension // This indicates to Jenkins that this is an implementation of
               // an extension point.
    public static final class DescriptorImpl
        extends BuildStepDescriptor<Publisher>
    {

        //~ Instance fields ----------------------------------------------------

        /**
         * To persist global configuration information, simply store it in a
         * field and call save().
         *
         * <p>If you don't want fields to be persisted, use {@code transient}.
         * </p>
         */
        private String electricFlowUrl;
        private String electricFlowUser;
        private String electricFlowPassword;

        //~ Constructors -------------------------------------------------------

        /**
         * In order to load the persisted global configuration, you have to call
         * load() in the constructor.
         */
        public DescriptorImpl()
        {
            load();
        }

        //~ Methods ------------------------------------------------------------

        @Override public boolean configure(
                StaplerRequest req,
                JSONObject     formData)
            throws FormException
        {
            electricFlowUrl      = formData.getString("electricFlowUrl");
            electricFlowUser     = formData.getString("electricFlowUser");
            electricFlowPassword = formData.getString("electricFlowPassword");

            save();

            return super.configure(req, formData);
        }

        public FormValidation doCheckConfiguration(
                @QueryParameter String value)
        {
            return Utils.validateValueOnEmpty(value, "Configuration");
        }

        public FormValidation doCheckFilePath(@QueryParameter String value)
        {
            return Utils.validateValueOnEmpty(value, "File path");
        }

        public ListBoxModel doFillConfigurationItems()
        {
            return Utils.fillConfigurationItems();
        }

        /**
         * This human readable name is used in the configuration screen.
         *
         * @return  this human readable name is used in the configuration
         *          screen.
         */
        @Override public String getDisplayName()
        {
            return
                "ElectricFlow - Create/Deploy Application from Deployment Package";
        }

        public String getElectricFlowPassword()
        {
            return electricFlowPassword;
        }

        public String getElectricFlowUrl()
        {
            return electricFlowUrl;
        }

        public String getElectricFlowUser()
        {
            return electricFlowUser;
        }

        @Override public boolean isApplicable(
                Class<? extends AbstractProject> aClass)
        {

            // Indicates that this builder can be used with all kinds of
            // project types
            return true;
        }
    }
}
