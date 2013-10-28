package DSLauncher.src;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

public class DSLauncherHead extends JFrame {
	/** Auto Generated ID **/
	private static final long serialVersionUID = 4255942636165851766L;
	
	/** Title Text **/
	private JLabel titleLabel;
	
	/** JLabel for a scrolling status indicator **/
	private JLabel statusLabel;
	
	/** String for the Status message **/
	private String statusString;

	/** Scroll Pane to mount statusLabel **/
	private JScrollPane scrollPane;
	
	/** JPanel to hold the scroll Pane **/
	private JPanel statusPanel;
	
	private String versionFromFile;
	private String versionFromServer;
		
	private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");
	private final String DEFAULT_FILE_NAME = "DSLauncher.properties";
	
	private final int DEFAULT_WIDTH = 400;
	private final int DEFAULT_HEIGHT = 300;
	
	private static final int GREATER = 1;
	private static final int EQUAL = 0;
	private static final int OUT_OF_DATE = -1;
	
	/** 
	 * Main method to be called externally.  Creates an instance of DSLauncherHead, which starts off this carnival ride!
	 * 
	 * @param args Command Line Arguments... currently unused
	 * TODO: Consider making a CLA to start the jar with the terminal in the background
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
		
		int versionStatus = compareVersion(versionFromFile, versionFromServer);
		if (versionStatus == GREATER) {
			//The file has a "more up to date version number than the Server"
			//No idea what to do here...
			setVisible(true);
			
			appendLine("Unexpected Version Mismatch");
			appendLine("Server Version: " + versionFromServer);
			appendLine("Local Version: " + versionFromFile);
			
			saveConsoleLog("DSUpdate Log " + dateFormat.format(new Date()) + ".txt");
			JOptionPane.showMessageDialog(null, "You appear to be a couple versions ahead of us.\nDid you modify " + DEFAULT_FILE_NAME + "?", "Version Mismatch", JOptionPane.WARNING_MESSAGE);
			closeGUI();
		}
		else if (versionStatus == OUT_OF_DATE) {
			appendLine("You're version is out of date!");
			//Turn the GUI on!
			setVisible(true);
			
			updateDSMinecraftInstallation();
			saveToTextFile(DEFAULT_FILE_NAME);
			
			saveConsoleLog("DSUpdate Log " + dateFormat.format(new Date()) + ".txt");
			
			JOptionPane.showMessageDialog(null, "Your version was updated to " + versionFromServer, "Updated", JOptionPane.PLAIN_MESSAGE);
			
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
		
		setLocationRelativeTo(null);
		
		//Do we want users to be able to resize the window/popup? 
		//setResizable(false);
	}
	
	/**
	 * Initialization routine, used to set instance variables
	 */
	private void init() {
		updateVersionFromServer();
		loadFromTextFile(DEFAULT_FILE_NAME);
	}
	
	/**
	 * An Unused empty Post Initialization routine
	 */
	private void postInit() {
		
	}
	
	private void closeGUI() {
		dispose();
		System.exit(1);
	}
	
	private void loadFromTextFile(String filename){
		try {
			Scanner scanner = new Scanner(new File(filename));
			
			versionFromFile = scanner.nextLine().trim();
			//TODO: Read more data than just the version number?

			
			scanner.close();
		} catch (FileNotFoundException ex) {
			//No file exists, program will make a new File, and set default values
			//TODO: Make necessary instance variables, and set them properly here
				//(Default private final variables would be expected)
			versionFromFile = versionFromServer;
			
			saveToTextFile(filename);
			appendLine(DEFAULT_FILE_NAME + " missing, a new one was created.");
		} catch (Exception ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(null, "File read error", "Error", JOptionPane.ERROR_MESSAGE);
			closeGUI();
		}
	}
	
	private void saveToTextFile(String filename){
		try{
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(filename)));
			
			out.println(versionFromServer);
			//TODO: Store more than just the version file?
			
			
			
			out.close();
		} catch (IOException ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(null, "File write error", "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private void saveConsoleLog(String filename) {
		try {
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(filename)));
			String output = statusLabel.getText().replace("<br>", "\r\n");	//Windows Specific Output Text
			output = output.replace("<html>", "");
			output = output.replace("</html>", "");
			
			out.print(output);
			out.close();
		} catch (IOException ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(null, "File write error", "Error", JOptionPane.ERROR_MESSAGE);
		}
		
	}
	
	private void updateVersionFromServer() {
		appendLine("Obtaining version from server.");
		int pingResponse;
		
		//TODO Actually get the Version from the Server
		versionFromServer = "1.0.0";
		pingResponse = -1;
		
		
		
		appendLine("Recieved server version reply: " + versionFromServer + ", " + pingResponse + "ms later.");
	}
	
	private int compareVersion(String vf, String vs) {
		appendLine("Comparing version.");
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
		appendLine("Updating from version: " + versionFromFile);
		//TODO: update the DSMinecraft Installation... I have no idea what this may entail
		
		
		
		versionFromFile = versionFromServer;
		appendLine("Updated to version: " + versionFromFile);
	}
	
	/**
	 * Appends the given string to the statusLabel JLabel
	 * @param str String to append
	 */
	private void appendLine(String str) {
		System.out.println(str);
		statusString += str + "<br>";
		statusLabel.setText("<html>" + statusString + "</html>");
	}
}
