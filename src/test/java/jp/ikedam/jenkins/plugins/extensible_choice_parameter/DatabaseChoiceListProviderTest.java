/*
 * The MIT License
 * 
 * Copyright (c) 2014 IKEDA Yasuyuki
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

import static org.junit.Assert.*;
import hudson.model.FreeStyleProject;
import hudson.model.ParametersDefinitionProperty;

import java.io.File;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;

import javax.sql.DataSource;

import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.pool.ObjectPool;
import org.jenkinsci.plugins.database.Database;
import org.jenkinsci.plugins.database.h2.LocalH2Database;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.jvnet.hudson.test.CaptureEnvironmentBuilder;

/**
 * Tests for {@link DatabaseChoiceListProvider}
 */
public class DatabaseChoiceListProviderTest
{
    @Rule
    public ExtensibleChoiceParameterJenkinsRule j = new ExtensibleChoiceParameterJenkinsRule();
    
    private File databaseDir = null;
    private Database database = null;
    
    @Before
    public void createDatabase() throws Exception
    {
        databaseDir = j.createTmpDir();
        database = new LocalH2Database(databaseDir);
    }
    
    /**
     * Release database pooling.
     * Otherwise tests may fail especially on Windows
     * as unable to delete database files.
     * 
     * @throws Exception
     */
    @After
    public void releaseDatabase() throws Exception
    {
        DataSource ds = database.getDataSource();
        if(ds instanceof PoolingDataSource)
        {
            Field f = PoolingDataSource.class.getDeclaredField("_pool");
            f.setAccessible(true);
            ObjectPool pool = (ObjectPool)f.get(ds);
            pool.close();
        }
    }
    
    @Test
    public void testNothing() throws Exception
    {
        Connection conn = database.getDataSource().getConnection();
        conn.close();
    }
    @Test
    public void testConfiguration() throws Exception
    {
        FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(
                new ExtensibleChoiceParameterDefinition(
                        "PARAM1",
                        new DatabaseChoiceListProvider(
                                new LocalH2Database(databaseDir),
                                "choiceValue",
                                "choiceTable",
                                "/path/to/some/file"
                        ),
                        false,
                        "test"
                )
        ));
        p.save();
        
        WebClient wc = j.createWebClient();
        
        j.submit(wc.getPage(p, "configure").getFormByName("config"));
        
        p = j.jenkins.getItemByFullName(p.getFullName(), FreeStyleProject.class);
        
        ExtensibleChoiceParameterDefinition def = (ExtensibleChoiceParameterDefinition)p
                .getProperty(ParametersDefinitionProperty.class)
                .getParameterDefinition("PARAM1");
        DatabaseChoiceListProvider dbProvider = (DatabaseChoiceListProvider)def.getChoiceListProvider();
        assertEquals(databaseDir, ((LocalH2Database)dbProvider.getDatabase()).getPath());
        assertEquals("choiceValue", dbProvider.getDbColumn());
        assertEquals("choiceTable", dbProvider.getDbTable());
        assertEquals("/path/to/some/file", dbProvider.getFallbackFilePath());
    }
    
    @Test
    public void testIntegration() throws Exception
    {
        Connection conn = database.getDataSource().getConnection();
        try
        {
            Statement st = conn.createStatement();
            st.execute(
                    "CREATE TABLE choiceTable("
                            + "choiceValue VARCHAR"
                    + ")"
            );
            st.close();
            
            PreparedStatement pst = conn.prepareStatement("INSERT INTO choiceTable(choiceValue) VALUES (?)");
            pst.setString(1, "value2");
            pst.execute();
            
            pst.setString(1, "value1");
            pst.execute();
            
            pst.setString(1, "value3");
            pst.execute();
            
            pst.close();
        }
        finally
        {
            conn.close();
        }
        
        FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(
                new ExtensibleChoiceParameterDefinition(
                        "PARAM1",
                        new DatabaseChoiceListProvider(
                                database,
                                "choiceValue",
                                "choiceTable",
                                "/path/to/some/file"
                        ),
                        false,
                        "test"
                )
        ));
        CaptureEnvironmentBuilder ceb = new CaptureEnvironmentBuilder();
        p.getBuildersList().add(ceb);
        
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        assertEquals("value1", ceb.getEnvVars().get("PARAM1"));
    }
    
    @Test
    public void testBasicBehavior() throws Exception
    {
        Connection conn = database.getDataSource().getConnection();
        try
        {
            Statement st = conn.createStatement();
            st.execute(
                    "CREATE TABLE choiceTable("
                            + "choiceString1 VARCHAR, "
                            + "choiceString2 VARCHAR, "
                            + "choiceInteger INTEGER"
                    + ")"
            );
            st.close();
            
            PreparedStatement pst = conn.prepareStatement(
                    "INSERT INTO choiceTable("
                            + "choiceString1, "
                            + "choiceString2, "
                            + "choiceInteger"
                     + ") VALUES (?, ?, ?)");
            pst.setString(1, "value1");
            pst.setString(2, "otherValue3");
            pst.setInt(3, 20);
            pst.execute();
            
            pst.setString(1, "value2");
            pst.setString(2, "otherValue2");
            pst.setInt(3, 10);
            pst.execute();
            
            pst.setString(1, "value3");
            pst.setString(2, "otherValue1");
            pst.setInt(3, 30);
            pst.execute();
            
            pst.close();
        }
        finally
        {
            conn.close();
        }
        {
            ChoiceListProvider provider = new DatabaseChoiceListProvider(
                    database,
                    "choiceString1",
                    "choiceTable",
                    "/path/to/some/file"
            );
            
            assertEquals(
                    Arrays.asList(
                        "value1",
                        "value2",
                        "value3"
                    ),
                    provider.getChoiceList()
            );
        }
        {
            ChoiceListProvider provider = new DatabaseChoiceListProvider(
                    database,
                    "choiceString2",
                    "choiceTable",
                    "/path/to/some/file"
            );
            
            assertEquals(
                    Arrays.asList(
                        "otherValue1",
                        "otherValue2",
                        "otherValue3"
                    ),
                    provider.getChoiceList()
            );
            
        }
        {
            ChoiceListProvider provider = new DatabaseChoiceListProvider(
                    database,
                    "choiceInteger",
                    "choiceTable",
                    "/path/to/some/file"
            );
            
            assertEquals(
                    Arrays.asList(
                        "10",
                        "20",
                        "30"
                    ),
                    provider.getChoiceList()
            );
            
        }
    }
    
    @Test
    public void testNoRecords() throws Exception
    {
        Connection conn = database.getDataSource().getConnection();
        try
        {
            Statement st = conn.createStatement();
            st.execute(
                    "CREATE TABLE choiceTable("
                            + "choiceValue VARCHAR"
                    + ")"
            );
            st.close();
        }
        finally
        {
            conn.close();
        }
        
        {
            ChoiceListProvider provider = new DatabaseChoiceListProvider(
                    database,
                    "choiceValue",
                    "choiceTable",
                    "/path/to/some/file"
            );
            
            assertEquals(
                    Collections.<String>emptyList(),
                    provider.getChoiceList()
            );
        }
    }
    
    @Test
    public void testNullValue() throws Exception
    {
        Connection conn = database.getDataSource().getConnection();
        try
        {
            Statement st = conn.createStatement();
            st.execute(
                    "CREATE TABLE choiceTable("
                            + "choiceValue VARCHAR"
                    + ")"
            );
            st.close();
            
            PreparedStatement pst = conn.prepareStatement("INSERT INTO choiceTable(choiceValue) VALUES (?)");
            
            pst.setString(1, null);
            pst.execute();
            
            pst.setString(1, "value1");
            pst.execute();
            
            pst.close();
        }
        finally
        {
            conn.close();
        }
        
        {
            ChoiceListProvider provider = new DatabaseChoiceListProvider(
                    database,
                    "choiceValue",
                    "choiceTable",
                    "/path/to/some/file"
            );
            
            assertEquals(
                    Arrays.asList("", "value1"),
                    provider.getChoiceList()
            );
        }
    }
}
