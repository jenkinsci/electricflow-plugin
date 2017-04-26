
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
import hudson.tasks.Builder;
import hudson.tasks.Publisher;

import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

/**
 * Sample {@link Builder}.
 *
 * <p>When the user configures the project and enables this builder, {@link
 * DescriptorImpl#newInstance(StaplerRequest)} is invoked and a new {@link
 * ElectricFlowPublishApplication} is created. The created instance is persisted
 * to the project configuration XML by using XStream, so this allows you to use
 * instance fields to remember the configuration.</p>
 *
 * <p>When a build is performed, the {@link #perform} method will be invoked.
 * </p>
 */
public class ElectricFlowPublishApplication
    extends Publisher
{

    //~ Static fields/initializers ---------------------------------------------

    private static final Log log = LogFactory.getLog(
            ElectricFlowPublishApplication.class);

    //~ Instance fields --------------------------------------------------------

    private final String credential;
    private String       filePath;

    //~ Constructors -----------------------------------------------------------

    @DataBoundConstructor public ElectricFlowPublishApplication(
            String credential,
            String artifactName,
            String filePath)
    {
        this.credential = credential;
        this.filePath   = filePath;
    }

    //~ Methods ----------------------------------------------------------------

    @Override public boolean perform(
            AbstractBuild build,
            Launcher      launcher,
            BuildListener listener)
    {
        FilePath workspace = build.getWorkspace();

        if (workspace == null) {
            log.warn("Workspace should not be null");

            return false;
        }

        String  workspaceDir = workspace.getRemote();
        Integer buildNumber  = build.getNumber();

        // do replace
        String newFilePath = filePath;

        newFilePath = newFilePath.replace("$BUILD_NUMBER",
                buildNumber.toString());

        // artifact version
        String artifactVersion = buildNumber.toString();

        try {

            // String workspaceDir, String filePath, String buildNumber
            makeApplicationArchive(workspaceDir, newFilePath);
        }
        catch (IOException e) {
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

            // String group, String key, String version, String file
            ElectricFlowConfigurationManager efCM     =
                new ElectricFlowConfigurationManager();
            Configuration                    cred     =
                efCM.getCredentialByName(credential);
            ElectricFlowClient               efClient = new ElectricFlowClient(
                    cred.getElectricFlowUrl(), cred.getElectricFlowUser(),
                    cred.getElectricFlowPassword(), workspaceDir);

            // efclient has been created
            efClient.uploadArtifact("default", artifactName, artifactVersion,
                "application.zip", true);
            deployResponse = efClient.deployApplicationPackage(artifactGroup,
                    artifactKey, artifactVersion, "application.zip");

            if (log.isDebugEnabled()) {
                log.debug("DeployApp response: " + deployResponse);
            }
        }
        catch (Exception e) {
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
    public String getCredential()
    {
        return credential;
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

    //~ Methods ----------------------------------------------------------------

    // }
    public static File createZipArchive(
            String basePath,
            String archiveName,
            String path)
        throws IOException
    {
        File f = new File(basePath + "/" + path);

        if (f.exists() && f.isDirectory()) {
            List<File> fileList = FileHelper.getFilesFromDirectoryWildcard(
                    basePath + "/" + path, "**");

            return createZipArchive(basePath + "/" + path, archiveName,
                fileList);
        }

        List<File> filesToArchive = FileHelper.getFilesFromDirectoryWildcard(
                basePath, path);

        return createZipArchive(basePath, archiveName, filesToArchive, true);
    }

    public static File createZipArchive(
            String   basePath,
            String   archiveName,
            String[] files)
        throws IOException
    {
        List<File> fileList = new ArrayList<>();

        for (int i = 0; i < files.length; i++) {
            File f = new File(files[i]);

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
        File            archive = new File(archiveName);
        ZipOutputStream out     = new ZipOutputStream(new FileOutputStream(
                    archive));

        if (cutTopLevelDir) {
            cutTopLevelDir = FileHelper.isTopLeveDirSame(files);
        }

        for (File row : files) {
            FileInputStream in = new FileInputStream(basePath + "/"
                        + row.getPath());

            try {
                String filePathToAdd = row.getPath();

                if (cutTopLevelDir) {
                    filePathToAdd = FileHelper.cutTopLevelDir(filePathToAdd);
                }

                out.putNextEntry(new ZipEntry(filePathToAdd));

                int    len;
                byte[] buf = new byte[1024];

                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }

                // Complete the entry
                out.closeEntry();
                in.close();
                out.closeEntry();
            }
            catch (IOException e) {
                in.close();
                throw new IOException("Unable to compress zip file: "
                        + basePath, e);
            }
        }

        out.close();

        return archive;
    }

    // This methods
    public static File makeApplicationArchive(
            String workspaceDir,
            String filePath)
        throws IOException
    {

        // in this method manifest is already tuned, so all we need is just to
        // package archive.
        String archivePath = workspaceDir + "/application.zip";

        return createZipArchive(workspaceDir, archivePath, filePath);
    }

    public static String getCurrentTimeStamp()
    {
        String dateFormat = "yyyy-MM-dd-HH-mm-ss.S";

        return new SimpleDateFormat(dateFormat).format(new Date());
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

            // To persist global configuration information, set that to
            // properties and call save(). useFrench =
            // formData.getBoolean("useFrench"); ^Can also use
            // req.bindJSON(this, formData); (easier when there are many fields;
            // need set* methods for this, like setUseFrench)
            electricFlowUrl      = formData.getString("electricFlowUrl");
            electricFlowUser     = formData.getString("electricFlowUser");
            electricFlowPassword = formData.getString("electricFlowPassword");

            save();

            return super.configure(req, formData);
        }

        public FormValidation doCheckCredential(@QueryParameter String value)
        {
            return Utils.validateValueOnEmpty(value, "Credential");
        }

        public FormValidation doCheckFilePath(@QueryParameter String value)
        {
            return Utils.validateValueOnEmpty(value, "File path");
        }

        public ListBoxModel doFillCredentialItems()
        {
            return Utils.fillCredentialItems();
        }

        /**
         * This human readable name is used in the configuration screen.
         *
         * @return  this human readable name is used in the configuration
         *          screen.
         */
        @Override public String getDisplayName()
        {
            return "ElectricFlow - Create/Deploy Application from Deployment Package";
        }

        public String getElectricFlowPassword()
        {
            return electricFlowPassword;
        }

        /**
         * This method returns true if the global configuration says we should
         * speak French.
         *
         * <p>The method name is bit awkward because global.jelly calls this
         * method to determine the initial state of the checkbox by the naming
         * convention.</p>
         *
         * @return  this method returns true if the global configuration says we
         *          should speak French.
         */
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
