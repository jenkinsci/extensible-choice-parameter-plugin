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

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.FormValidation;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jenkins.model.Jenkins;

import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.DirectoryScanner;
import org.jvnet.localizer.Localizable;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 * Create a choice list from a list of files.
 */
public class FilenameChoiceListProvider extends ChoiceListProvider
{
    private static final long serialVersionUID = 1329937323978223039L;
    
    /**
     * A type specifying what type of files to scan to list.
     */
    public enum ScanType
    {
        File(Messages._FilenameChoiceListProvider_ScanType_File()),
        Directory(Messages._FilenameChoiceListProvider_ScanType_Directory()),
        FileAndDirectory(Messages._FilenameChoiceListProvider_ScanType_FileAndDirectory());
        
        private Localizable name;
        
        private ScanType(Localizable name)
        {
            this.name = name;
        }
        
        @Override
        public String toString()
        {
            return name.toString();
        }
    };
    
    public enum EmptyChoiceType
    {
        None(Messages._FilenameChoiceListProvider_EmptyChoiceType_None()),
        AtTop(Messages._FilenameChoiceListProvider_EmptyChoiceType_AtTop()),
        AtEnd(Messages._FilenameChoiceListProvider_EmptyChoiceType_AtEnd());
        
        private Localizable name;
        
        private EmptyChoiceType(Localizable name)
        {
            this.name = name;
        }
        
        @Override
        public String toString()
        {
            return name.toString();
        }
    }
    
    private final String baseDirPath;
    
    /**
     * Returns a path to a directory to scan for files.
     * 
     * If this path is absolute, it is used as specified.
     * If this path is relative, it is considered relative from JENKINS_HOME.
     * 
     * @return the baseDirPath
     */
    public String getBaseDirPath()
    {
        return baseDirPath;
    }
    
    /**
     * Returns a directory to scan for files from the passed path.
     * 
     * Whether the path is relative or absolute, is resolved here.
     * 
     * @param baseDirPath a path string to the directory.
     * @return A file object specifying the proper path of the directory.
     */
    protected static File getBaseDir(String baseDirPath)
    {
        File rawDir = new File(baseDirPath);
        if(rawDir.isAbsolute())
        {
            return rawDir;
        }
        
        return new File(Jenkins.getInstance().getRootDir(), baseDirPath);
    }
    
    /**
     * Returns a directory to scan for files.
     * 
     * @return A file object specifying the proper path to the directory to scan.
     */
    protected File getBaseDir()
    {
        return getBaseDir(getBaseDirPath());
    }
    
    private final String includePattern;
    
    /**
     * Returns a pattern for files to include.
     * 
     * @return the include pattern
     */
    public String getIncludePattern()
    {
        return includePattern;
    }
    
    
    private final ScanType scanType;
    
    /**
     * Returns what type of files to list.
     * 
     * @return what type of files to list
     */
    public ScanType getScanType()
    {
        return scanType;
    }
    
    
    private final String excludePattern;
    
    /**
     * Returns a pattern for files to exclude.
     * 
     * @return the exclude pattern
     */
    public String getExcludePattern()
    {
        return excludePattern;
    }
    
    private final boolean reverseOrder;
    
    /**
     * @return
     */
    public boolean isReverseOrder()
    {
        return reverseOrder;
    }
    
    private final EmptyChoiceType emptyChoiceType;
    
    /**
     * @return whether and where to put an empty choice.
     */
    public EmptyChoiceType getEmptyChoiceType()
    {
        return emptyChoiceType;
    }
    
    /**
     * The constructor called when a user posts a form.
     * 
     * @param baseDirPath a path to the directory to scan.
     * @param includePattern a pattern of file names to include to the list.
     * @param excludePattern a pattern of file names to exclude from the list.
     * @param scanType a type of files to list.
     * @param emptyChoiceType whether and where to put an empty choice.
     */
    @DataBoundConstructor
    public FilenameChoiceListProvider(String baseDirPath, String includePattern, String excludePattern, ScanType scanType, boolean reverseOrder, EmptyChoiceType emptyChoiceType)
    {
        this.baseDirPath = StringUtils.trim(baseDirPath);
        this.includePattern = StringUtils.trim(includePattern);
        this.excludePattern = StringUtils.trim(excludePattern);
        this.scanType = scanType;
        this.reverseOrder = reverseOrder;
        this.emptyChoiceType = emptyChoiceType;
    }
    
    @Deprecated
    public FilenameChoiceListProvider(String baseDirPath, String includePattern, String excludePattern, ScanType scanType, boolean reverseOrder)
    {
        this(baseDirPath, includePattern, excludePattern, scanType, reverseOrder, EmptyChoiceType.None);
    }
    
    /**
     * For backward compatibility.
     * @param baseDirPath
     * @param includePattern
     * @param excludePattern
     * @param scanType
     * @deprecated use {@link FilenameChoiceListProvider#FilenameChoiceListProvider(String, String, String, ScanType, boolean)}
     */
    @Deprecated
    public FilenameChoiceListProvider(String baseDirPath, String includePattern, String excludePattern, ScanType scanType)
    {
        this(baseDirPath, includePattern, excludePattern, scanType, false);
    }
    
    /**
     * List files from passed parameters.
     * 
     * @param baseDir
     * @param includePattern
     * @param excludePattern
     * @param scanType
     * @param reverseOrder
     * @return
     */
    protected static List<String> getFileList(
            File baseDir,
            String includePattern,
            String excludePattern,
            ScanType scanType,
            boolean reverseOrder,
            EmptyChoiceType emptyChoiceType
    )
    {
        if(baseDir == null || !baseDir.exists() || !baseDir.isDirectory())
        {
            return new ArrayList<String>(0);
        }
        if(StringUtils.isBlank(includePattern))
        {
            return new ArrayList<String>(0);
        }
        if(scanType == null)
        {
            scanType = ScanType.File;
        }
        
        DirectoryScanner ds = new DirectoryScanner();
        ds.setBasedir(baseDir);
        ds.setIncludes(includePattern.split("\\s*,(?:\\s*,)*\\s*"));
        if(!StringUtils.isBlank(excludePattern))
        {
            ds.setExcludes(excludePattern.split("\\s*,(?:\\s*,)*\\s*"));
        }
        ds.scan();
        
        List<String> ret = null;
        
        switch(scanType)
        {
        case FileAndDirectory:
            {
                ret = new ArrayList<String>(ds.getIncludedDirsCount() + ds.getIncludedFilesCount());
                for(String file: ds.getIncludedFiles())
                {
                    ret.add(file);
                }
                for(String dir: ds.getIncludedDirectories())
                {
                    ret.add(dir);
                }
                Collections.sort(ret);
                break;
            }
        case Directory:
            {
                ret = new ArrayList<String>(Arrays.asList(ds.getIncludedDirectories()));
                break;
            }
        default:
            {
                // case File:
                ret = new ArrayList<String>(Arrays.asList(ds.getIncludedFiles()));
                break;
            }
        }
        
        ret = new ArrayList<String>(ret);
        
        if(reverseOrder)
        {
            Collections.reverse(ret);
        }
        
        if(emptyChoiceType == null)
        {
            emptyChoiceType = EmptyChoiceType.None;
        }
        
        switch(emptyChoiceType)
        {
        case None:
            break;
        case AtTop:
            ret.add(0, "");
            break;
        case AtEnd:
            ret.add("");
            break;
        }
        
        return ret;
    }
    
    @Deprecated
    protected static List<String> getFileList(
            File baseDir,
            String includePattern,
            String excludePattern,
            ScanType scanType,
            boolean reverseOrder
    )
    {
        return getFileList(baseDir, includePattern, excludePattern, scanType, reverseOrder, EmptyChoiceType.None);
    }
    
    /**
     * For backward compatibility.
     * 
     * @param baseDir
     * @param includePattern
     * @param excludePattern
     * @param scanType
     * @return
     * @deprecated use {@link FilenameChoiceListProvider#getFileList(File, String, String, ScanType, boolean)}
     */
    @Deprecated
    protected static List<String> getFileList(
            File baseDir,
            String includePattern,
            String excludePattern,
            ScanType scanType
    )
    {
        return getFileList(baseDir, includePattern, excludePattern, scanType, false);
    }
    
    /**
     * Returns the list of choices to show.
     * 
     * @return
     * @see jp.ikedam.jenkins.plugins.extensible_choice_parameter.ChoiceListProvider#getChoiceList()
     */
    @Override
    public List<String> getChoiceList()
    {
        return getFileList(
               getBaseDir(),
               getIncludePattern(),
               getExcludePattern(),
               getScanType(),
               isReverseOrder(),
               getEmptyChoiceType()
       );
    }
    
    /**
     * Class for view.
     */
    @Extension
    static public class DescriptorImpl extends Descriptor<ChoiceListProvider>
    {
        /**
         * Returns the name for displaying.
         * 
         * @return the name for displaying
         */
        @Override
        public String getDisplayName()
        {
            return Messages.FilenameChoiceListProvider_DisplayName();
        }
        
        /**
         * Validate a value inputed for baseDirPath
         * 
         * Checks followings:
         * * not blank
         * * specified path exists
         * * specified path is a directory.
         * 
         * @param baseDirPath
         * @return FormValidation object
         */
        public FormValidation doCheckBaseDirPath(@QueryParameter String baseDirPath)
        {
            if(StringUtils.isBlank(baseDirPath))
            {
                return FormValidation.error(Messages.FilenameChoiceListProvider_BaseDirPath_empty());
            }
            
            File baseDir = getBaseDir(baseDirPath);
            if(!baseDir.exists() || !baseDir.isDirectory())
            {
                return FormValidation.warning(Messages.FilenameChoiceListProvider_BaseDirPath_empty());
            }
            
            return FormValidation.ok();
        }
        
        /**
         * Validate a value inputed for includePattern
         * 
         * Checks followings:
         * * not blank
         * 
         * @param includePattern
         * @return FormValidation object
         */
        public FormValidation doCheckIncludePattern(@QueryParameter String includePattern)
        {
            if(StringUtils.isBlank(includePattern))
            {
                return FormValidation.error(Messages.FilenameChoiceListProvider_IncludePattern_empty());
            }
            
            return FormValidation.ok();
        }
        
        
        /**
         * Validate a value inputed for excludePattern
         * 
         * always ok.
         * 
         * @param excludePattern
         * @return FormValidation object
         */
        public FormValidation doCheckExcludePattern(@QueryParameter String excludePattern)
        {
            return FormValidation.ok();
        }
        
        /**
         * Test what files will be listed.
         * 
         * @param baseDirPath
         * @param includePattern
         * @param excludePattern
         * @param scanType
         * @return
         */
        public FormValidation doTest(
                @QueryParameter String baseDirPath,
                @QueryParameter String includePattern,
                @QueryParameter String excludePattern,
                @QueryParameter ScanType scanType,
                @QueryParameter boolean reverseOrder,
                @QueryParameter EmptyChoiceType emptyChoiceType
        )
        {
            List<String> fileList = getFileList(
                    getBaseDir(baseDirPath),
                    includePattern,
                    excludePattern,
                    scanType,
                    reverseOrder,
                    emptyChoiceType
            );
            
            if(fileList.isEmpty())
            {
                return FormValidation.ok("(No file matched)");
            }
            
            return FormValidation.ok(StringUtils.join(fileList, '\n'));
        }
    }
}
