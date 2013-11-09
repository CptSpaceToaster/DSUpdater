package DSLauncher.src;

import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.Scanner;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import net.sf.sevenzipjbinding.ISevenZipInArchive;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import net.sf.sevenzipjbinding.simple.ISimpleInArchive;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;

public class DSLauncherHead extends JFrame {
	/** Auto Generated ID **/
	private static final long serialVersionUID = 4255942636165851766L;
	
	/* GUI Stuff */
	private JLabel titleLabel;
	private JLabel statusLabel;
	private JScrollPane scrollPane;
	private JPanel statusPanel;
	
	/* Instance Variables */
	private URL updateUrl;
	
	private String statusString;
	private String versionFromFile;
	private String greatestVersionFromServer;
	
	private int numOfUpdates;
	
	private ArrayList<String> versions;
	private ArrayList<String> downloadUrls; 
	private ArrayList<String> fileNames;
	
	/* Comparison Definitions */
	private static final int GREATER = 1;
	private static final int EQUAL = 0;
	private static final int OUT_OF_DATE = -1;
	
	
	
	/***********************/
	/* START CONFIGURATION */
	/***********************/
	
	/** Location of update.txt **/
	private final String UPDATE_URL_STRING = "https://dl.dropboxusercontent.com/s/n4jfufpyh5emqg1/fakeUpdate.txt"; //"https://dl.dropboxusercontent.com/u/5921811/update.txt"
	
	/** File Date Format **/
	private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");
	
	/** DS Update Log Filename (The Date will be Appended onto the end of this **/
	private final String UPDATE_FILENAME = "DS Update Log ";
	
	/** DS Error Log Filename (The Date will be Appended onto the end of this **/
	private final String ERROR_FILENAME = "DS Error Log ";
	
	/** DS Launcher Properties Filename **/
	private final String DEFAULT_FILE_NAME = "DSLauncher.properties";
	
	/** Default Version Number (incase one doesn't exist) **/
	private static final String DEFAULT_VERSION = "";
	
	/** Pixel Width of Window **/
	private final int DEFAULT_WIDTH = 400;
	
	/** Pixel Height of Window **/
	private final int DEFAULT_HEIGHT = 300;
	
	/** Whether or not the user can Resize the console window **/
	private final boolean isConsoleResizable = false;
	
	/** File Download buffer **/
	private final int BUFFER_SIZE = 4096; //4kb buffer
	
	/*********************/
	/* END CONFIGURATION */
	/*********************/
	
	
	/** 
	 * Main method to be called externally.  Creates an instance of DSLauncherHead, which starts off this carnival ride!
	 * 
	 * @param args Command Line Arguments... currently unused
	 * TODO: Consider making a CLA to start the jar with the terminal in the background, or always show its log.
	 */
	public static void main(String[] args) {
		new DSLauncherHead();
	}
	
	/**
	 * This is the main bulk of the program.  Init routines in the beginning, and updates the files appropriatly.
	 */
	private DSLauncherHead() {
		preInit();
		init();
		postInit();
		
		int versionStatus = compareVersion(versionFromFile, greatestVersionFromServer);
		if (versionStatus == GREATER) {
			//The file has a "more up to date version number than the Server"
			//No idea what to do here...
			setVisible(true);
			
			appendLine("Unexpected Version Mismatch");
			appendLine("Server Version: " + greatestVersionFromServer);
			appendLine("Local Version: " + versionFromFile);
			
			saveConsoleLog(UPDATE_FILENAME + dateFormat.format(new Date()) + ".txt");
			JOptionPane.showMessageDialog(null, "You appear to be a couple versions ahead of us.\nDid you modify " + DEFAULT_FILE_NAME + "?", "Version Mismatch", JOptionPane.WARNING_MESSAGE);
			closeGUI();
		}
		else if (versionStatus == OUT_OF_DATE) {
			appendLine("You're version is out of date!");
			//Turn the GUI on!
			setVisible(true);
			updateDSMinecraftInstallation();
			//saveToTextFile(DEFAULT_FILE_NAME);
			
			saveConsoleLog(UPDATE_FILENAME + dateFormat.format(new Date()) + ".txt");
			
			JOptionPane.showMessageDialog(null, "Your version was updated to " + greatestVersionFromServer, "Updated", JOptionPane.PLAIN_MESSAGE);
			
		}
		else if (versionStatus == EQUAL) {	
			appendLine("You are up to date!");
		}
		
		//TODO: Launch Minecraft
		


		closeGUI();
	}

	/**
	 * Pre-Inititialization routine, used for variable initialization
	 */
	private void preInit() {
		statusString = "";
		versions = new ArrayList<String>();
		downloadUrls = new ArrayList<String>();
		fileNames = new ArrayList<String>();
		
		titleLabel = new JLabel("DSLauncher");
		titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
		statusLabel = new JLabel(statusString);
		scrollPane = new JScrollPane();
		statusPanel = new JPanel();
		
		statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.PAGE_AXIS));
		statusPanel.add(statusLabel);
		scrollPane.add(statusPanel);
		
		setLayout(new BorderLayout());
		add(titleLabel, BorderLayout.NORTH);
		add(statusPanel, BorderLayout.CENTER);
		
		//Removes the TitleBar, the X, minimize, and maximize buttons
		setUndecorated(true);
		getRootPane().setWindowDecorationStyle(JRootPane.NONE);
		//The X doesn't exist any more, but I set the default close operation anyways...
		setDefaultCloseOperation (JFrame.EXIT_ON_CLOSE);
		setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
		//Start window in the center of the main screen
		setLocationRelativeTo(null);
		
		setResizable(isConsoleResizable);
		
		try {
			updateUrl = new URL(UPDATE_URL_STRING);
		} catch (MalformedURLException e) {
			//Uhhhh... it broke?
			appendLine("Error: URL could not be made");
			saveConsoleLog(ERROR_FILENAME + dateFormat.format(new Date()) + ".txt");
		}
	}
	
	/**
	 * Initialization routine, used to set instance variables
	 */
	private void init() {
		loadFromTextFile(DEFAULT_FILE_NAME);
		getVersionsFromServer();
	}
	
	/**
	 * Nothing at the moment
	 */
	private void postInit() {
	}
	
	private void closeGUI() {
		dispose();
		System.exit(1);
	}
	
	private void loadFromTextFile(String filename){
		Scanner s = null;
		try {
			s = new Scanner(new File(filename));
			
			versionFromFile = s.nextLine().trim();
			//TODO: Read more data than just the version number?

			
			
		} catch (FileNotFoundException e) {
			//No file exists, program will make a new File, and set default values
			//TODO: Make necessary instance variables, and set them properly here
				//(Default private final variables would be expected)
			versionFromFile = DEFAULT_VERSION;
			
			saveToTextFile(filename);
			appendLine(DEFAULT_FILE_NAME + " missing, a new one was created.");
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "File read error", "Error", JOptionPane.ERROR_MESSAGE);
			closeGUI();
		}
		
		if (s != null) {
			s.close();
		}
	}
	
	private void saveToTextFile(String filename){
		PrintWriter out = null;
		try{
			out = new PrintWriter(new BufferedWriter(new FileWriter(filename)));
			
			out.println(greatestVersionFromServer);
			//TODO: Store more than just the version file?
			
			
			
			
		} catch (IOException ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(null, "File write error", "Error", JOptionPane.ERROR_MESSAGE);
		}
		
		if (out != null) {
			out.close();
		}
	}
	
	private void saveConsoleLog(String filename) {
		try {
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(filename)));
			String output = statusLabel.getText().replace("<br>", System.lineSeparator());
			output = output.replace("<html>", "");
			output = output.replace("</html>", "");
			
			out.print(output);
			out.close();
		} catch (IOException ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(null, "File write error", "Error", JOptionPane.ERROR_MESSAGE);
		}
		
	}
	
	/**
	 * Checks the update.txt file for Versions, and if it finds versions that are out of date, then it adds them to the
	 * lists to update!
	 * 
	 *  Modifies:
	 *  greatestVersionFromServer - When it's through, this will represent the latest version released by the server!
	 *  ArrayList<String> versions
	 *  ArrayList<String> downloadUrls
	 *  ArrayList<String> fileNames
	 */
	private void getVersionsFromServer() {
		appendLine("Obtaining version from server.");
		numOfUpdates = 0;
		
		BufferedReader in = null;
		try {
			in = new BufferedReader(new InputStreamReader(updateUrl.openStream()));
			appendLine("Recieved server version reply.");
			
		    try {
		    	String line;
		    	while ((line = in.readLine()) != null) {
			    	//Splits the string into parts based on semi colons, and creates empty strings if there exists ;;
			    	String[] updateParts = line.replace(" ", "").split(";", -1);
			    	
		    		if ((updateParts.length == 3) && compareVersion(updateParts[0], versionFromFile) == GREATER) {
		    			appendLine("Found Version: " + updateParts[0]);
		    			
		    			versions.add(updateParts[0]);
				    	downloadUrls.add(updateParts[1]);
				    	fileNames.add(updateParts[2]);
				    	
				    	numOfUpdates++;
				    	System.out.println(numOfUpdates);
		    		}
		    	}
		    	//Assume the latest Version is the Last Version
		    	if (numOfUpdates != 0)
		    		greatestVersionFromServer = versions.get(numOfUpdates-1);
		    } catch(IndexOutOfBoundsException e) {
		    	e.printStackTrace();
				appendLine("Update.txt doesn't look right.");
				appendLine("Expected: Version; DownloadURL; FileName\n");
				appendLine("Ignoring all updates and ignoring check.");
		    }
		} catch(Exception e) {
			appendLine("Couldn't recieve response from server, ignoring updates");
			greatestVersionFromServer = versionFromFile;
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
					appendLine("We can't seem to close the download properly... ");
				}
			}
		}
	}
	
	/**
	 * Compares two version Strings lexicographically.  Longer Versions (Parts separated by periods) are considered "more up to date"
	 * 1.1.0 is greater than 1.0.0
	 * 1.1.0 is greater than 1.0.200A
	 * a.0.0 is greater than 1.1.1
	 * 1.0.0 is greater than 9.9
	 * 
	 * @param vf First Version String
	 * @param vs Second Version String
	 * @return Whether the first version is greater, than the second version, equal to, or out of date.
	 */
	private int compareVersion(String vf, String vs) {
		//Split by periods, needed escape sequence \\.
		String[] vfParts = vf.split("\\.");
		String[] vsParts = vs.split("\\.");
		
		if (vfParts.length>vsParts.length) {
			/* Version number from the file was "longer" (More numbers separated by a period)
			 * than the version from the server.  Assume the file is messed up, but somehow "more up to date" */
			 return GREATER;
		} else if (vfParts.length<vsParts.length) {
			/* Version number from server was "longer" (More numbers separated by a period)
			 * than the version from the text file.  Assume the server is up to date. */
			 return OUT_OF_DATE;
		} else {
			//Compare all parts with each other, starting with the leftmost parts
			for (int i=0; i<vfParts.length; i++) {
				//Compares the two parts lexicographically.
				int result = vfParts[i].compareTo(vsParts[i]);
				if (result > 0) {
					return GREATER;
				} else if (result < 0) {
					return OUT_OF_DATE;
				}
			}
			
			return EQUAL;
		}
	}
	
	private void updateDSMinecraftInstallation() {
		for (int i=0; i<numOfUpdates; i++) {
			appendLine("Updating from version: " + versionFromFile);
			//TODO: update the DSMinecraft Installation... I have no idea what this may entail
	//		ISevenZipInArchive inArchive = null;
	//		RandomAccessFile r = null;
	//        Scanner s = null;
	//        URL d = null;
	        
	        appendLine("Downloading Update: " + versions.get(i));
	        appendLine(downloadUrls.get(i));
	        
	        try{
		        downloadFromURL(new URL(downloadUrls.get(i)), fileNames.get(i));
		        
		        
	//	        inArchive = SevenZip.openInArchive(null, // autodetect archive type
	//	                new RandomAccessFileInStream(r));
	//        
	//        	//Getting simple interface of the archive inArchive
	//        	ISimpleInArchive simpleInArchive = inArchive.getSimpleInterface();
	        
	        } catch(Exception e) {
	        	System.out.println("IT ALL BLOWED UP!");
	        	e.printStackTrace();
	        } finally {
	        	//Close Files
	        }
			
			
			
			versionFromFile = greatestVersionFromServer;
			appendLine("Updated to version: " + versionFromFile);
		}
	}
	
	/**
	 * Appends the given string to the statusLabel JLabel
	 * @param str String to append
	 */
	private void appendLine(String str) {
		System.out.println(str);
		str.replace("\n", "<br>");
		statusString += str + "<br>";
		statusLabel.setText("<html>" + statusString + "</html>");
	}
	
	void downloadFromURL(URL url, String localFilename) throws IOException {
	    InputStream i = null;
	    FileOutputStream f = null;
	
	    try {
	        URLConnection urlConn = url.openConnection();//connect
	
	        i = urlConn.getInputStream();               //get connection inputstream
	        f = new FileOutputStream(localFilename);   //open outputstream to local file
	
	        int downloaded = 0;
	        int fileSize = i.available();
	        
	        byte[] buffer = new byte[BUFFER_SIZE];
	        int len;
	
	        //while we have availble data, continue downloading and storing to local file
	        while ((len = i.read(buffer)) > 0) {  
	            f.write(buffer, 0, len);
	            downloaded += len;
	            System.out.println(downloaded + "/" + fileSize + " kB");
	        }
	    } finally {
	        try {
	            if (i != null) {
	                i.close();
	            }
	        } finally {
	            if (f != null) {
	                f.close();
	            }
	        }
	    }
	}
}
