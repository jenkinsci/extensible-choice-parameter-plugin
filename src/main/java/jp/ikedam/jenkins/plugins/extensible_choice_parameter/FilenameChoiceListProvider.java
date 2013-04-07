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
import java.io.Serializable;
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
public class FilenameChoiceListProvider extends ChoiceListProvider implements Serializable
{
    private static final long serialVersionUID = 1329937323978223039L;
    
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
    
    private String baseDirPath;
    
    /**
     * @return the baseDirPath
     */
    public String getBaseDirPath()
    {
        return baseDirPath;
    }
    
    protected static File getBaseDir(String baseDirPath)
    {
        File rawDir = new File(baseDirPath);
        if(rawDir.isAbsolute())
        {
            return rawDir;
        }
        
        return new File(Jenkins.getInstance().getRootDir(), baseDirPath);
    }
    
    protected File getBaseDir()
    {
        return getBaseDir(getBaseDirPath());
    }
    
    private String includePattern;
    
    /**
     * @return the includePattern
     */
    public String getIncludePattern()
    {
        return includePattern;
    }
    
    
    private ScanType scanType;
    
    /**
     * @return the scanType
     */
    public ScanType getScanType()
    {
        return scanType;
    }
    
    
    /**
     * @return the excludePattern
     */
    public String getExcludePattern()
    {
        return excludePattern;
    }
    
    
    private String excludePattern;
    
    @DataBoundConstructor
    public FilenameChoiceListProvider(String baseDirPath, String includePattern, String excludePattern, ScanType scanType)
    {
        this.baseDirPath = StringUtils.trim(baseDirPath);
        this.includePattern = StringUtils.trim(includePattern);
        this.excludePattern = StringUtils.trim(excludePattern);
        this.scanType = scanType;
    }
    
    
    protected static List<String> getFileList(
            File baseDir,
            String includePattern,
            String excludePattern,
            ScanType scanType
    )
    {
        if(StringUtils.isBlank(includePattern))
        {
            return new ArrayList<String>(0);
        }
        
        DirectoryScanner ds = new DirectoryScanner();
        ds.setBasedir(baseDir);
        ds.setIncludes(StringUtils.split(includePattern, ','));
        if(!StringUtils.isBlank(excludePattern))
        {
            ds.setExcludes(StringUtils.split(excludePattern, ','));
        }
        ds.scan();
        
        switch(scanType)
        {
        case FileAndDirectory:
            {
                List<String> ret = new ArrayList<String>(ds.getIncludedDirsCount() + ds.getIncludedFilesCount());
                for(String file: ds.getIncludedFiles())
                {
                    ret.add(file);
                }
                for(String dir: ds.getIncludedDirectories())
                {
                    ret.add(dir);
                }
                Collections.sort(ret);
                return ret;
            }
        case Directory:
            return Arrays.asList(ds.getIncludedDirectories());
        default:
            return Arrays.asList(ds.getIncludedFiles());
        }
    }
    
    /**
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
               getScanType()
       );
    }
    
    @Extension
    static public class DescriptorImpl extends Descriptor<ChoiceListProvider>
    {
        @Override
        public String getDisplayName()
        {
            return Messages.FilenameChoiceListProvider_DisplayName();
        }
        
        public FormValidation doCheckBaseDirPath(@QueryParameter String baseDirPath)
        {
            // TODO
            return FormValidation.ok();
        }
        
        public FormValidation doCheckIncludePattern(@QueryParameter String includePattern)
        {
            // TODO
            return FormValidation.ok();
        }
        
        
        public FormValidation doCheckExcludePattern(@QueryParameter String excludePattern)
        {
            // TODO
            return FormValidation.ok();
        }
        
        public FormValidation doTest(
                @QueryParameter String baseDirPath,
                @QueryParameter String includePattern,
                @QueryParameter String excludePattern,
                @QueryParameter ScanType scanType
        )
        {
            List<String> fileList = getFileList(
                    getBaseDir(baseDirPath),
                    includePattern,
                    excludePattern,
                    scanType
            );
            
            if(fileList.isEmpty())
            {
                return FormValidation.ok("(No file matched)");
            }
            
            return FormValidation.ok(StringUtils.join(fileList, '\n'));
        }
    }
}
