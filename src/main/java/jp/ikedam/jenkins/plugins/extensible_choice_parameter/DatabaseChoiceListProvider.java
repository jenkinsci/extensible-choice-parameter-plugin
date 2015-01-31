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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.database.Database;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 * Create a choice list by querying a specific column of a table in a database.
 * 
 * @author Thibault Duchateau
 */
public class DatabaseChoiceListProvider extends ChoiceListProvider implements Serializable
{
	private static final long serialVersionUID = 8488443485003760275L;
        
    private static final Logger LOGGER = Logger.getLogger(DatabaseChoiceListProvider.class.getName());
    
    private final Database database;
    private final String dbTable;
    private final String dbColumn;
    private final String fallbackFilePath;
    
    public Database getDatabase()
    {
        return database;
    }

	public String getDbColumn() {
		return dbColumn;
	}

	public String getDbTable() {
		return dbTable;
	}

	public String getFallbackFilePath() {
		return fallbackFilePath;
	}

	@Override
	public List<String> getChoiceList() {
		List<String> allChoices = performDbQuery();
		LOGGER.log(Level.CONFIG, "Returned " + allChoices.size() + " entries from the database");
		return allChoices;
	}
	
	@DataBoundConstructor
    public DatabaseChoiceListProvider(Database database, String dbColumn, String dbTable, String fallbackFilePath)
    {
        this.database = database;
        this.dbColumn = StringUtils.trim(dbColumn);
        this.dbTable = StringUtils.trim(dbTable);
        this.fallbackFilePath = StringUtils.trim(fallbackFilePath);
    }
	
	@Extension(optional=true)  // available only when database-plugin is installed.
    public static class DescriptorImpl extends Descriptor<ChoiceListProvider>
    {
        /**
         * the display name shown in the dropdown to select a choice provider.
         * 
         * @return display name
         * @see hudson.model.Descriptor#getDisplayName()
         */
        @Override
        public String getDisplayName()
        {
            return Messages.DbChoiceListProvider_DisplayName();
        }
        
        /**
         * Validate a value inputed for colomndb
         * 
         * Checks followings:
         * * not blank
         * 
         * @param colomndb
         * @return FormValidation object
         */
        public FormValidation doCheckColomndb(@QueryParameter String colomndb)
        {
            if(StringUtils.isBlank(colomndb))
            {
                return FormValidation.error(Messages.DbChoiceListProvider_dbColumn_empty());
            }
                       
            return FormValidation.ok();
        }
        
        /**
         * Validate a value inputed for tabledb
         * 
         * Checks followings:
         * * not blank
         * 
         * @param tabledb
         * @return FormValidation object
         */
        public FormValidation doCheckTabledb(@QueryParameter String tabledb)
        {
            if(StringUtils.isBlank(tabledb))
            {
                return FormValidation.error(Messages.DbChoiceListProvider_dbTable_empty());
            }
                       
            return FormValidation.ok();
        }
        
    }
	
	
	/**
	 * Recupere la liste des environnements recuperer de la base de donnees JACK	  
	 *	 
	 */
	/**
	 * 
	 * @return
	 */
	private List<String> performDbQuery(){

		Connection conn = null;
		ResultSet resultat = null;
		List<String> resultList = new ArrayList<String>();
		try {
			String colomndb = getDbColumn();
			if (colomndb == null || StringUtils.isEmpty(colomndb)) {
				LOGGER.log(Level.WARNING,"Invalid column");
			}
			
			String tabledb = getDbTable();
			if (tabledb == null || StringUtils.isEmpty(tabledb)) {
				LOGGER.log(Level.WARNING,"table database invalid");				
			}
			
			conn = getDatabase().getDataSource().getConnection();
						
			// By default, a SELECT * is performed
			String selectQuery = "select " + colomndb + " from " + tabledb;
						
			// Use plain old JDBC to build and execute the query against the configured db
			Statement statement = conn.createStatement();
			boolean result = statement.execute(selectQuery);
			if(result){
				resultat = statement.executeQuery(selectQuery);
				while(resultat.next()) {
					resultList.add(resultat.getString(1));				  
				}
			}else{
				LOGGER.log(Level.WARNING,"No result found with the query: " + selectQuery);
			}
		}
		catch (SQLException se) {
			LOGGER.log(Level.SEVERE,String.format("Unable to access the database: %s (%s)", getDatabase().toString(), getDatabase().getDescriptor().getDisplayName()), se);			
		}
		finally {
			try {
				if (conn != null) {
					conn.close();
				}
			}
			catch (SQLException se) {
				LOGGER.log(Level.SEVERE,"Fermeture de connection impossible, SQLException", se);
			}
		}
		
		// If no results are returned, read values from the fallback file if configured
		if(resultList.isEmpty()){
		
			Scanner scanner = null;
			try {
				scanner = new Scanner(new FileInputStream(getFallbackFilePath()));
				while(scanner.hasNextLine()) {
					String env = scanner.nextLine().trim();
					if(StringUtils.isNotBlank(env)){
						resultList.add(env);
					}
				}
			} catch (FileNotFoundException e) {				
				LOGGER.log(Level.WARNING,"Unable to read the fallback file: ", e);				
			}
			finally {
				if(scanner != null){
					scanner.close();
				}
			}
		}
		
		// Perform alphabetical sorting on results
		Collections.sort(resultList);
		
		return resultList;
	}
}
