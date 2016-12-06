package com.framework.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import com.framework.handlers.XMLHandler;
import io.appium.java_client.android.AndroidDriver;
import io.selendroid.common.SelendroidCapabilities;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.openqa.selenium.Platform;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.w3c.dom.*;

public class Driver {
		
	//Variables
	String rootPath;
	String executionPath;
	String storagePath;
	String enviromentsPath;
	String envConfigPath;
	String curExecutionFolder;
	String htmlReportsPath;
	String snapShotsPath;
	String dataPath;
	String dataSheet;
	String configXML;
	String User;	
	
	HashMap <String, String> orgDictionary = new HashMap<String, String>();
	HashMap <String, String> dictionary = new HashMap<String, String>();
	HashMap <String, String> environment = new HashMap<String, String>();

	//Constructor
	public Driver(HashMap <String, String> dictionary, HashMap <String, String> environment)
	{
		
		this.dictionary = dictionary;
		this.environment = environment;
		
		//Get Root Path
		User = System.getProperty("user.name");
		String workingPath = System.getProperty("user.dir");
		rootPath = workingPath.split("trunk")[0];
		
		//Set paths
		executionPath = rootPath + "/Execution";
		storagePath = rootPath;
		dataPath = storagePath + "/data";
		enviromentsPath = storagePath + "/environments/Environments.xls";
		envConfigPath = storagePath + "/environments/EnvConfig.xml";
		
		dataSheet = dataPath + "/" + this.environment.get("CLASSNAME") + ".xls";
		configXML = storagePath + "/config/config.xml";
				
		//Add to Env Variables
		this.environment.put("ROOTPATH", rootPath);
		this.environment.put("EXECUTIONFOLDERPATH", executionPath);
		this.environment.put("STORAGEFOLDERPATH", storagePath);
		this.environment.put("ENVIRONMENTXLSPATH", enviromentsPath);
		
	}
	
	//Function to Create Execution Folders
	public boolean createExecutionFolders() throws IOException
	{		
		//Set execution paths
		curExecutionFolder = executionPath + "/" + environment.get("ENV_CODE") + "/" + environment.get("CLASSNAME");
		htmlReportsPath = curExecutionFolder + "/" + environment.get("BROWSER").toUpperCase() + "_HTML_Reports";
		snapShotsPath = htmlReportsPath + "/Snapshots";
		String harFilePath = htmlReportsPath + "/Hars";
		
		//Put in Environments
		environment.put("CURRENTEXECUTIONFOLDER", curExecutionFolder);
		environment.put("HTMLREPORTSPATH", htmlReportsPath);
		environment.put("SNAPSHOTSFOLDER", snapShotsPath);
		environment.put("HARFOLDER", harFilePath);

		//Delete if folder already exists
		if (new File(htmlReportsPath).exists())
			deleteFile(new File(htmlReportsPath));
		if ((new File(snapShotsPath)).mkdirs() && (new File(harFilePath)).mkdirs()) return true;
		else return false;
	}
	
	//*****************************************************************************************
    //*    Name        		: deleteFile
    //*****************************************************************************************
    public static void deleteFile(File file)
	throws IOException{
    	  
        if(file.isDirectory()){

            String files[] = file.list();

            for (String temp : files) {
                File fileDelete = new File(file, temp);
                deleteFile(fileDelete);
            }

            if(file.list().length == 0) file.delete();
        }

        else file.delete();
    }
    
    //*****************************************************************************************
    //*    Name        	  : fetchEnvironmentDetails
    //*****************************************************************************************
	public boolean fetchEnvironmentDetails() {

		try {
		    int iVersion = -1;
		    int iEnvironment = -1;
		    boolean bFlag = false;

		    //Get the Column Index for the ENVIRONMENT Column
		    iEnvironment = getColumnIndex(enviromentsPath, "ENVIRONMENTS", "ENVIRONMENT");
		    
		    //Check if the index value is proper
		    if (iEnvironment == -1 ){
		    	System.out.println("Failed to find the ENVIRONMENT Column in the file " + enviromentsPath);
		    	return false;
		    }
		    
			//Create the FileInputStream object
			FileInputStream file = new FileInputStream(new File(enviromentsPath));
		    HSSFWorkbook workbook = new HSSFWorkbook(file);
		    HSSFSheet sheet = workbook.getSheet("ENVIRONMENTS");
		    
		    //Get the Number of Rows
		    int iRowNum = sheet.getLastRowNum();
		    
		    //Get the Column count
		    int iColCount = sheet.getRow(0).getLastCellNum();
		    
		    for (int iRow = 0; iRow <= iRowNum; iRow++)
		    {
	            //Check if the version and the envioronment value is matching
	            //String strVersion = sheet.getRow(iRow).getCell(iVersion).getStringCellValue().trim().toUpperCase();
	            String strEnvironment = sheet.getRow(iRow).getCell(iEnvironment).getStringCellValue().trim().toUpperCase();
	            //Currently checking only on the basis of envrionment
	            if (!strEnvironment.equals(environment.get("ENV_CODE")))
	            {
	            	continue;
	            }
	            
	            //Set the flag value to true
	            bFlag = true;
	            String strKey = "";
	            String strValue = "";
		        //Loop through all the columns
		        for (int iCell = 0; iCell < iColCount; iCell ++ )
		        {
                    //Put the Details in environment Hashmap
		            strKey = sheet.getRow(0).getCell(iCell).getStringCellValue().trim().toUpperCase();
		            
	        		//Fetch the value for the Header Row
	        		if (sheet.getRow(iRow).getCell(iCell, org.apache.poi.ss.usermodel.Row.CREATE_NULL_AS_BLANK) == null)
	        		{
	        			strValue = "";
	        		}else{
	        			strValue = sheet.getRow(iRow).getCell(iCell).getStringCellValue();
	        		}

	        		environment.put(strKey.trim(), strValue.trim());
		        }
		        break;
		    }
		    //Close the file
		    file.close();

		    //If bFlag is true
		    if (bFlag == false)
		    {
		    	System.out.println("environment Code " + environment.get("ENV_CODE") + " not found in the environment xls");
		    	return false;
		    }

		    return true;
		     
		} catch (FileNotFoundException e) {
		    e.printStackTrace();
		    return false;
		} catch (IOException e) {
		    e.printStackTrace();
		    return false;
		}
	}

	//*****************************************************************************************
	//*    Name        	  : fetchEnvironmentDetailsFromConfigXML
	//*****************************************************************************************
	public boolean fetchEnvironmentDetailsFromConfigXML() {

		try{
			Document doc = XMLHandler.getXMLDocument(envConfigPath);
			if(doc == null) return false;

			Element elemEnvironment = XMLHandler.getElementByName(doc, environment.get("ENV_CODE").toLowerCase());
			if(elemEnvironment == null){
				System.out.println("Unable to find Env node with ID " + environment.get("ENV_CODE"));
				return false;
			}

			List<Element> Parameters = XMLHandler.getChildElements(elemEnvironment);

			for(Element Parameter : Parameters)
					environment.put(Parameter.getTagName().trim().toUpperCase(), Parameter.getTextContent().trim());

			return true;

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	//*****************************************************************************************
    //*	Name		    : getColumnIndex
    //*	Description	    : Function to get the Column Index
    //*	Author		    : Aniket Gadre
    //* Input Params	: int row - Row number to skip
    //*	Return Values	: None
    //***********************************************************************
	public int getColumnIndex(String strXLS, String strSheetName, String strColumnName)
	{
		try
		{
			//Create the FileInputStream obhect			
			FileInputStream file = new FileInputStream(new File(strXLS));		     
		    //Get the workbook instance for XLS file 
		    HSSFWorkbook workbook = new HSSFWorkbook(file);
		 
		    //Get first sheet from the workbook
		    HSSFSheet sheet = workbook.getSheet(strSheetName);
		     
		    //Iterate through each rows from first sheet
		    Row row = sheet.getRow(0);
		    
		    //Get the Column count
		    int iColCount = row.getLastCellNum();
		    int iCell = 0;
		    int iIndex = -1;
	        String strTemp = "";

	        //Loop through all the columns
	        for (iCell = 0; iCell < iColCount; iCell ++)
	        {
        		//Get the index for Version and Enviornment
        		strTemp = sheet.getRow(0).getCell(iCell).getStringCellValue().trim().toUpperCase();
        		
        		//if the strColumnName contains Header then check for HEADER or HEADER_IND
        		if (strColumnName.equals("HEADER_IND") || strColumnName.equals("HEADER"))
        		{
        			if (strTemp.equals("HEADER") || strTemp.equals("HEADER_IND"))
        			{
        				iIndex = iCell;
        				//Exit the Loop
        				break;
        			}
        			
        		}else{ 
        			if (strTemp.equals(strColumnName.trim().toUpperCase()))
        			{
        				iIndex = iCell;
        				//Exit the Loop
        				break;
        			}
        		}
	        }
	        //Close the file
		    file.close();
		    
		    //Validate if index is returned properly or not
		    if (iIndex != -1){
		    	return iIndex;
		    }
            else{
		    	System.out.println("Failed to find the Column Id for Column " + strColumnName);
		    	return -1;
		    }
			
		}
        catch (Exception e){
			System.out.println("Got exception while finding the Index column. Exception is " + e);
			return -1;
		}
	}

	//*****************************************************************************************
    //*	Name		    : getDataForTest
    //***********************************************************************
	public boolean getDataForTest(String testName)
	{
		//DataSheet
		final String dataSheet = dataPath + "/" + environment.get("CLASSNAME") + ".xls";
		final String mainSheet = "MAIN"; 
		final String testNameColumn = "TEST_NAME";
		
		//Clear dictionary
		dictionary.clear();
		orgDictionary.clear();
		
		//Get column index of test name column
	    int iColTestName = getColumnIndex(dataSheet, mainSheet, testNameColumn);
	    
	    try{
	    	//Create the FileInputStream obhect			
			FileInputStream file = new FileInputStream(new File(dataSheet));
		    HSSFWorkbook workbook = new HSSFWorkbook(file);
		    HSSFSheet sheet = workbook.getSheet(mainSheet);
		     
		    //Iterate through each rows from first sheet
		    int iRowCnt = sheet.getLastRowNum();

		    //Loop
		    int iRow;
		    for(iRow=0;iRow<iRowCnt;iRow++){
		    	//Get row with test name and exist
		    	if(sheet.getRow(iRow).getCell(iColTestName).getStringCellValue().equalsIgnoreCase(testName)) break;
		    }
		    
		    //Check if test found
		    if(iRow == iRowCnt){
		    	System.out.println("Test with name: " + testName + " not found in datasheet: " +  dataSheet);
		    	return false;
		    }
		    		    
		    //Set Header & DataRow
		    Row headerRow = sheet.getRow(iRow-1);
		    Row dataRow = sheet.getRow(iRow);
		    
		    //Get Column count for test-1 row
		    int iParamCnt = headerRow.getLastCellNum();		 
		    
		    //
		    String key="";
		    String value="";
		    		    
		    //Loop through params
		    int iCol;
		    for(iCol=0;iCol<iParamCnt;iCol++){
		    	
		    	//Fetch the value for the Header Row
        		if (headerRow.getCell(iCol, org.apache.poi.ss.usermodel.Row.CREATE_NULL_AS_BLANK) == null)
        		{
        			key = "";
        		}else{
        			key = headerRow.getCell(iCol).getStringCellValue();
        		}
		    	
        		//Fetch the value for the Header Row
        		if (dataRow.getCell(iCol, org.apache.poi.ss.usermodel.Row.CREATE_NULL_AS_BLANK) == null)
        		{
        			value = "";
        		}else{
        			value = dataRow.getCell(iCol).getStringCellValue();
        		}
        		
        		//Check key value
        		if(key.isEmpty()) break;
        		else {
        			dictionary.put(key,value);
        			orgDictionary.put(key,value);
        		}
		    	
		    }
		    
		    //Give call to get Reference data to replace & parameters
		    if (getReferenceData() == false) return false;
		    			    		   
	        return true;
	    }
	    catch(Exception e)
	    {
	    	System.out.println("Exception " + e + " occured while fetching data from datasheet " + dataSheet + " for test " + testName);
	    	return false;
	    }		
	}
	
		//*****************************************************************************************
	   //*	Name		    : getReferenceData
	   //*****************************************************************************************	
		public boolean getReferenceData()
		{
			//Declare few variables
			String key, value;
			Map.Entry me;
			
			//DataSheet			
			final String krSheet = "KEEP_REFER"; 
			
			//Get a set of the entries 
			Set set = dictionary.entrySet();
	   	
		   	//Get an iterator 
		   	Iterator i = set.iterator(); 
		   	try
	   		{
			    int iRow = 0;
			    String strKeyName = "";
			    String strKeyValue = "";
			    boolean bFoundFlag = false;			    
			    
		        //Set KEY_VALUE column as per Test Name
		        String colName = environment.get("BROWSER").toUpperCase() + "_KEY_VALUE";
		        
		        //Call the function to get the Column Index in the KEEP_REFER sheet
		        int iColIndex = getColumnIndex(dataSheet, krSheet, colName);
		        
		        //Validate if we get the index
		        if (iColIndex == -1){
		        	System.out.println("Failed to find the " + colName + " Column in the KEEP_REFER sheet in file " + dataSheet);
			    	return false;
		        }
		        
				//Create the FileInputStream obhect			
				FileInputStream file = new FileInputStream(new File(dataSheet));
			    HSSFWorkbook workbook = new HSSFWorkbook(file);
			    HSSFSheet sheet = workbook.getSheet(krSheet);
			    
			    //Get the RowNum
			    int iRowNum = sheet.getLastRowNum();
			    
			    //Looping through the iterator
			   	while(i.hasNext()) 
			   	{
		    		me = (Map.Entry)i.next();
		    		key = me.getKey().toString();
		    		value = me.getValue().toString();
		    		
		    		//If we need to get data from KEEP refer sheet
		    		if (value.startsWith("&",0))
		    		{
		    			value = value.substring(1);
		    			
		    			//Loop thorugh all the rows in the KEEP_REFER sheet
		    		    for (iRow = 0; iRow <= iRowNum; iRow++)
		    		    {
		    		    	strKeyName = "";
		    		    	strKeyValue = "";
		    		    	bFoundFlag = false;
		    		    	
                            //Check if the key is present in the Keep_Refer sheet
                            try {
                                if (sheet.getRow(iRow).getCell(0, org.apache.poi.ss.usermodel.Row.CREATE_NULL_AS_BLANK) == null) {
                                    strKeyName = "";
                                }else{
                                    strKeyName = sheet.getRow(iRow).getCell(0).getStringCellValue();
                                }
                            }catch (NullPointerException e){
                                strKeyName = "";
                            }

                            //Check if the key matches the expected key
                            if (strKeyName.equals(value)) {
                                //Set the boolean value to true
                                bFoundFlag = true;

                                //Fetch the corresponding value
                                if (sheet.getRow(iRow).getCell(iColIndex, org.apache.poi.ss.usermodel.Row.CREATE_NULL_AS_BLANK) == null)
                                {
                                    strKeyValue = "";
                                }else{
                                    strKeyValue = sheet.getRow(iRow).getCell(iColIndex).getStringCellValue();
                                }

                                break;
                            }
		    		    }
		    		    
		    		    //If the Key is not found then check the same in environment hashmap
		    		    if (bFoundFlag){
		    		    	System.out.println("Key " + value + " found in KEEP_REFER and value is " + strKeyValue);
		    		    }else{
		    		    	//Fetch the value from the environment Hashmap
		    		    	System.out.println("Key " + value + " not found in KEEP_REFER sheet. Checking in the Enviornment Hashmap");
		    		    	strKeyValue = environment.get(value);
		    		    	System.out.println("Key " + value + " found in environment hashmap and value is " + strKeyValue);
		    		    }
	    			
		    	        //Check if Key value is not null
		    		    if (strKeyValue == null) {
		    		    	System.out.println("Value for Key " + value + " is null and not found in the KEEP_REFER sheet and in the environment hashmap");
		    		    	return false;
		    		    }
		    		    //Assign the value to dictionary key
		    	        me.setValue(strKeyValue);

		    		}else if (value.startsWith("@",0)){
		    	        me.setValue(value); 			
		    		}
		   		}
			   	
			   	//Close the file
			   	file.close();
			   	return true;
	   		}
	   		catch (Exception err) {
	        	System.out.print("Exception " + err + " occured in getReferenceData");
	        	err.printStackTrace();
	        	return false;
	   		}
	   }	
		
	//*****************************************************************************************
   //*	Name		    : setReferenceData
   //*	Description	    : Set the data in keep refer sheet
   //*	Author		    : Aniket Gadre
   //*	Input Params	: None
   //*	Return Values	: Boolean 
   //*****************************************************************************************	
	public boolean setReferenceData()
	{
		//Declare few variables
		String key, value, tempKey, tempValue;
	   	Map.Entry me;
	   	int Field_Count;
	   	
	    //DataSheet
		final String krSheet = "KEEP_REFER"; 
		final String colName = environment.get("BROWSER").toUpperCase() + "_KEY_VALUE";
   	
		//Get a set of the entries 
	   	Set set = orgDictionary.entrySet();
	   	
	   	//Get an iterator 
	   	Iterator i = set.iterator(); 
	   	
	   	try
   		{	 
		    int iRow = 0;
		    String strKeyName = "";
		    boolean bFoundFlag = false;

	        //Call the function to get the Column Index in the KEEP_REFER sheet
	        int iColIndex = getColumnIndex(dataSheet, krSheet, colName);
	        
	        //Validate if we get the index
	        if (iColIndex == -1){
	        	System.out.println("Failed to find the " + colName + " Column in the KEEP_REFER sheet in file " + dataSheet);
		    	return false;
	        }
	        
			//Create the FileInputStream obhect			
			FileInputStream file = new FileInputStream(new File(dataSheet));
		    HSSFWorkbook workbook = new HSSFWorkbook(file);
		    HSSFSheet sheet = workbook.getSheet(krSheet);
		    
		    //Get the RowNum
		    int iRowNum = sheet.getLastRowNum();

		   	//Looping through the iterator
		   	while(i.hasNext()) 
		   	{ 	
	   		
	    		me = (Map.Entry)i.next();
	    		key = me.getKey().toString();
	    		value = me.getValue().toString();
	    		
	    		//System.out.println("orgKey: " + key + "  orgValue: " + value);
	    		
	    		//If we need to get data from KEEP refer sheet
	    		if (value.startsWith("@",0))
	    		{
	    			tempKey = value.substring(1);
	    			
	    			//if dictionary item has been changed from the objGlobalDictOriginal item
	    			if (!(dictionary.get(key)).equalsIgnoreCase(orgDictionary.get(key).substring(1)))
	    			{
	    				tempValue = dictionary.get(key);
	    			}
	    			else
	    			{
	    				tempValue ="";
	    			}
	    		
	    			//Loop thorugh all the rows in the KEEP_REFER sheet
	    		    for (iRow = 0; iRow <= iRowNum; iRow++)
	    		    {
	    		    	strKeyName = "";
	    		    	bFoundFlag = false;
		        		try {
    		        		//Check if the key is present in the Keep_Refer sheet
    		        		if (sheet.getRow(iRow).getCell(0, org.apache.poi.ss.usermodel.Row.CREATE_NULL_AS_BLANK) == null) {
    		        			strKeyName = "";
    		        		}else
    		        		{
    		        			strKeyName = sheet.getRow(iRow).getCell(0).getStringCellValue();
    		        		}
		        		}catch (NullPointerException e) {
		        			strKeyName = "";
		        		}
		        		
		        		//Check if the key matches the expected key
		        		if (strKeyName.equals(tempKey)) {
		        			//Set the boolean value to true
		        			bFoundFlag = true;
		        			break;
		        		}
	    		    }
	    		    
	    		    //If the Key is present then update the value else add the key value in KEEP_REFER
	    		    if (bFoundFlag){
	    		    	//Update the key value in KEEP_REFER
    		    		sheet.getRow(iRow).createCell(iColIndex);
    		    		sheet.getRow(iRow).getCell(iColIndex).setCellValue(tempValue);
	    		    }else{
	    		    	//Set value for iRowNum
	    		    	iRowNum = iRowNum + 1;
	    		    	sheet.createRow(iRowNum);
	    		    	sheet.getRow(iRowNum).createCell(0);
	    		    	sheet.getRow(iRowNum).createCell(iColIndex);
	    		    	//Add the key and the value in KEEP_REFER sheet
	    		    	sheet.getRow(iRowNum).getCell(0).setCellValue(tempKey);
	    		    	sheet.getRow(iRowNum).getCell(iColIndex).setCellValue(tempValue);
	    		    }
	    		}
	   		}
		   	

		   	file.close();
		   	
		   	//Save the file
		    //Save the Changes made 
		    FileOutputStream outFile =new FileOutputStream(new File(dataSheet));
		    workbook.write(outFile);
		    outFile.close();
	    			
		   	return true;
	
   		}catch (Exception err)
   		{
   			System.out.print("Exception " + err + " occured in setReferenceData");
   			err.printStackTrace();
   			return false;
   		}
		
   }
	
   //*****************************************************************************************
   //*	Name		    : getEnv
   //*	Description	    : Returns the current environment
   //*	Author		    : Aniket Gadre
   //*	Input Params	: None
   //*	Return Values	: WebDriver 
   //*****************************************************************************************
	public String getEnv(){
				
		try{	
			File fXmlFile = new File(configXML);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder;
			dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
						
			doc.getDocumentElement().normalize();
			
			//Get Env Node
			NodeList nEnv = doc.getElementsByTagName("env");
			return ((Element)nEnv.item(0)).getTextContent();
			
		}
		catch(Exception e){
			e.printStackTrace();			
		}
		
		return "";
	}
	
	
   //*****************************************************************************************
   //*	Name		    : getWebDriver
   //*	Description	    : Returns the required webdriver
   //*	Author		    : Aniket Gadre
   //*	Input Params	: None
   //*	Return Values	: WebDriver 
   //*****************************************************************************************
	public WebDriver getWebDriver(String browser) throws MalformedURLException
	{
		String osName = System.getProperty("os.name");
        String webDriverType = browser;

		System.out.println("Executing Tests on OS " + osName);

        if (webDriverType.equalsIgnoreCase("firefox") || webDriverType.isEmpty()){

			return new FirefoxDriver();
        }
        else if (webDriverType.equalsIgnoreCase("chrome")){
			if(osName.toLowerCase().contains("linux")) {
				System.setProperty("webdriver.chrome.driver", storagePath + "/drivers/chromedriver");
				return new ChromeDriver();
			}
			else {
				System.setProperty("webdriver.chrome.driver", storagePath + "/drivers/chromedriver.exe");
				return new ChromeDriver();
			}
        }
        else if (webDriverType.equalsIgnoreCase("ie")){
            System.setProperty("webdriver.ie.driver", storagePath + "/drivers/IEDriverServer32.exe");
            return new InternetExplorerDriver();
        }
        else{
            System.out.println("Driver type " + webDriverType + " is invalid");
            return null;
        }
	}

	//*****************************************************************************************
	//*	Name		    : getRemoteWebDriver
	//*****************************************************************************************
	public WebDriver getRemoteWebDriver(String URL, DesiredCapabilities dc) throws MalformedURLException {
		return new RemoteWebDriver(new URL(URL),dc);
	}

    //*****************************************************************************************
    //*	Name		    : getAppiumAndroidDriver
    //*****************************************************************************************
	public AndroidDriver getAppiumAndroidDriver(String appPackage, String appActivity, String deviceName, String appiumServerURL) throws MalformedURLException {
		//Desired Caps
		DesiredCapabilities DC = new DesiredCapabilities();
		DC.setCapability("automationName", "Appium");
		DC.setCapability("platformName", "Android");
		DC.setCapability("appPackage", appPackage);
		DC.setCapability("appActivity", appActivity);
		DC.setCapability("deviceName", deviceName);

		//Initiate WebDriver
		return new AndroidDriver(new URL(appiumServerURL), DC);
	}

    //*****************************************************************************************
    //*	Name: getAndroidChromeDriver
    //*****************************************************************************************
	public AndroidDriver getAndroidChromeDriver(String deviceName, String appiumServerURL, Proxy proxy) throws MalformedURLException {
		//Desired Caps
		DesiredCapabilities DC = new DesiredCapabilities();
		DC.setCapability("automationName", "Appium");
		DC.setCapability("platformName", "Android");
		DC.setCapability("browserName", "Chrome");
		DC.setCapability("deviceName", deviceName);

		if(proxy != null) DC.setCapability(CapabilityType.PROXY,proxy);

		//Initiate WebDriver
		return new AndroidDriver(new URL(appiumServerURL), DC);
	}

    //*****************************************************************************************
    //*	Name: getSelendroidDriver
    //*****************************************************************************************
	public AndroidDriver getSelendroidDriver(String apkPath, String appPackage, String appWaitActivity, String deviceName, String appiumServerURL) throws MalformedURLException {
		//Selendroid Caps
		SelendroidCapabilities DC = new SelendroidCapabilities(appPackage);
		DC.setCapability("appWaitActivity",appWaitActivity);
		DC.setCapability("deviceName",deviceName);
		DC.setCapability("app",apkPath);
		DC.setCapability("automationName", "Selendroid");
		DC.setCapability("platformName", "Android");
		DC.setCapability("newCommandTimeout",3000);

		return new AndroidDriver(new URL(appiumServerURL), DC);
	}

    //*****************************************************************************************
    //*	Name: getSelendroidDriver
    //*****************************************************************************************
	public Platform getPlatform(String platformName){
		String osName = platformName.toUpperCase();
		if(osName.equals("WIN8.1")) return Platform.WIN8_1;
		else if (osName.equals("WIN8")) return Platform.WIN8;
		else if (osName.equals("ANDROID")) return Platform.ANDROID;
		else if (osName.equals("LINUX")) return Platform.LINUX;
		else if (osName.equals("MAC")) return Platform.MAC;
		else if (osName.equals("WIN")) return Platform.WINDOWS;
		else return Platform.ANY;
	}
}