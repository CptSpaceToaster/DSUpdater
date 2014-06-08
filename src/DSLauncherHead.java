package src;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Scanner;
import java.util.regex.Matcher;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRootPane;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sf.sevenzipjbinding.*;

public class DSLauncherHead extends JFrame {
	/* Auto Generated ID */
	private static final long serialVersionUID = 4255942636165851766L;

	/* GUI Stuff */
	private JPanel downloadPanel;
	private JPanel updatePanel;
	private JProgressBar downloadProgress;
	private ImageIcon background;
	private ImageIcon logo;
	
	/* Instance Variables */
	private URL updateUrl;

	private String statusString;
	private String updateUrlString;
	private String versionFromFile;
	private String greatestVersionFromServer;

	private int numOfUpdates;
	private int posX;
	private int posY;
	
	private ArrayList<String> versions;
	private ArrayList<String> downloadUrls;
	private ArrayList<String> fileNames;

	boolean success = true;

	/* Comparison Definitions */
	private static final int GREATER = 1;
	private static final int EQUAL = 0;
	private static final int OUT_OF_DATE = -1;

	/***********************/
	/* START CONFIGURATION */
	/***********************/

	/** File Date Format **/
	private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");

	/** System Application Name/Filename **/
	private final String name = "DSUpdater.jar";

	/** Folder for Log Files **/
	private final String LOG_FOLER_NAME = "logs";
	
	/** DS Update Log Filename (The Date will be Appended onto the end of this **/
	private final String UPDATE_FILENAME_LOCATION = LOG_FOLER_NAME + File.separator + "DS Update Log ";

	/** DS Error Log Filename (The Date will be Appended onto the end of this **/
	private final String ERROR_FILENAME_LOCATION = LOG_FOLER_NAME + File.separator + "DS Error Log ";

	/** DS Launcher Properties Filename **/
	private final String DEFAULT_FILE_NAME = "version.txt";

	/** DS Launcher Blacklist Filename **/
	private final String DEFAULT_BLACKLIST_NAME = ".files.blacklist";

	/** Default Version Number **/
	private final String DEFAULT_VERSION = "";

	/** Background image file **/
	private final String backgroundImageLocation = "assets/DSUpdater" + new Random().nextInt(5) + ".png";
	
	/** Background image file **/
	private final String logoImageLocation = "assets/DSIcon.png";
	
	/** Pixel Width of Window **/
	private final int DEFAULT_WIDTH = 646;

	/** Pixel Height of Window **/
	private final int DEFAULT_HEIGHT = 211;

	/** File Download buffer **/
	private final int BUFFER_SIZE = 4096; // 4kb buffer

	/** Choose to display "popup" update messages **/
	private final boolean displayUpdateMessages = false;

	/** Choose to display "popup error messages **/
	private final boolean displayErrorMessages = true;

	/*********************/
	/* END CONFIGURATION */
	/*********************/

	/**
	 * Main method to be called externally. Creates an instance of
	 * DSLauncherHead, which starts off this carnival ride!
	 * 
	 * @param args Command Line Arguments... currently unused TODO: Consider
	 *             making a CLA to start the jar with the terminal in the
	 *             background, or always show its log.
	 */
	public static void main(String[] args) {
		new DSLauncherHead();
	}

	/**
	 * This is the main bulk of the program. Init routines in the beginning, and
	 * updates the files appropriately.
	 */
	private DSLauncherHead() {
		preInit();
		init();
		postInit();

		int versionStatus = compareVersion(versionFromFile, greatestVersionFromServer);

		if (versionStatus == GREATER) {
			setVisible(true);

			appendLine("Unexpected Version Mismatch");
			appendLine("Server Version: " + greatestVersionFromServer);
			appendLine("Local Version: " + versionFromFile);

			saveConsoleLog(ERROR_FILENAME_LOCATION + dateFormat.format(new Date()) + ".txt");
			if (displayErrorMessages)
				JOptionPane.showMessageDialog(null,
						"You appear to be a couple versions ahead of us." + System.lineSeparator() + "Did you modify "
								+ DEFAULT_FILE_NAME + "?", "Version Mismatch",
						JOptionPane.WARNING_MESSAGE);
			closeGUI();
		} else if (versionStatus == OUT_OF_DATE) {

			updateDSMinecraftInstallation();
			saveToTextFile(DEFAULT_FILE_NAME);

			if (success) {
				saveConsoleLog(UPDATE_FILENAME_LOCATION + dateFormat.format(new Date()) + ".txt");
				if (displayUpdateMessages)
					JOptionPane.showMessageDialog(null, "Your version was updated to " +
							greatestVersionFromServer, "Updated", JOptionPane.PLAIN_MESSAGE);
			} else {
				saveConsoleLog(ERROR_FILENAME_LOCATION + dateFormat.format(new Date()) + ".txt");
				if (displayErrorMessages)
					JOptionPane.showMessageDialog(null, name + " has Encountered an Error", "Warning", JOptionPane.WARNING_MESSAGE);
			}

		} else if (versionStatus == EQUAL && success) {
			appendLine("You are up to date!");
		}
		
		closeGUI();
	}

	/**
	 * Pre-Inititialization routine, used for variable initialization and
	 * instantiation
	 */
	private void preInit() {
		new File(LOG_FOLER_NAME).mkdir();
		
		statusString = "";
		versions = new ArrayList<String>();
		downloadUrls = new ArrayList<String>();
		fileNames = new ArrayList<String>();
		
		UIManager.put("ProgressBar.selectionBackground", Color.DARK_GRAY);
		UIManager.put("ProgressBar.selectionForeground", new Color(75,75,75));
		UIManager.put("ProgressBar.horizontalSize", new Dimension(DEFAULT_WIDTH-100, 20));
		
		
		downloadProgress = new JProgressBar();
		downloadProgress.setSize(new Dimension(DEFAULT_WIDTH-100, 20));
		downloadProgress.setIndeterminate(true);
		downloadProgress.setStringPainted(true);
		downloadProgress.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
		    	downloadProgress.setString(String.format("Downloading: " + "" + " %s%%",
		    			downloadProgress.getValue()));
		    }
		  });
		
		downloadPanel = new JPanel();
		updatePanel = new DrawBackground();
		
		
		
		    
		
		// Set the download Panel to be transparent
		downloadPanel.setBackground(new Color(0,0,0,0));
		downloadPanel.setOpaque(false);
		
		// Add the JProgressBar to the panel
		downloadPanel.add(downloadProgress);
		
		// Add the Download Panel to the Frame's panel
		updatePanel.setLayout(new BorderLayout());
		updatePanel.add(downloadPanel,BorderLayout.SOUTH);
		
		// Add the Frame's panel to the frame
		add(updatePanel);
		
		// Removes the TitleBar, the X, minimize, and maximize buttons
		setUndecorated(true);
		getRootPane().setWindowDecorationStyle(JRootPane.NONE);
		// The X doesn't exist any more, but I set the default close operation
		// anyways...
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		// Transparent JFrame Background
		setBackground(new Color(0,0,0,0));
		
		// Frame Size
		setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
		
		// Start window in the center of the main screen
		setLocationRelativeTo(null);
		
		//Add Mouse Dragging support!
		this.addMouseListener(new MouseAdapter()
		{
		   public void mousePressed(MouseEvent e)
		   {
		      posX=e.getX();
		      posY=e.getY();
		   }
		});
		
		this.addMouseMotionListener(new MouseAdapter()
		{
		     public void mouseDragged(MouseEvent evt)
		     {
				//sets frame position when mouse dragged			
				setLocation (evt.getXOnScreen()-posX,evt.getYOnScreen()-posY);
							
		     }
		});
	}

	/**
	 * Initialization routine, used to gather the version from the Text file,
	 * and sets up the URL from the value from the text file.
	 */
	private void init() {
		loadFromTextFile(DEFAULT_FILE_NAME);

		try {
			updateUrl = new URL(updateUrlString);
		} catch (MalformedURLException e) {
			// Uhhhh... it broke? No really, if this breaks, I think You need to
			// reinstall Java...
			// Check the spelling on the updateUrl... it might be wrong?
			appendLine("Error: URL could not be made.  Double check " + DEFAULT_FILE_NAME + " to see if it's spelled properly");
			appendLine(e.getMessage());
			saveConsoleLog(ERROR_FILENAME_LOCATION + dateFormat.format(new Date()) + ".txt");
			success = false;
		}
		getVersionsFromServer();
	}

	/**
	 * Initializes the 7zip archive
	 */
	private void postInit() {
		try {
			SevenZip.initSevenZipFromPlatformJAR();
		} catch (SevenZipNativeInitializationException e) {
			appendLine("Error initializing 7Zip");
			appendLine(e.getMessage());
			success = false;
		}
	}

	private void closeGUI() {
		dispose();
		System.exit(1);
	}

	private void loadFromTextFile(String filename) {
		Scanner s = null;
		try {
			s = new Scanner(new File(filename));
			versionFromFile = s.nextLine().trim();
			appendLine("Found local version file: " + versionFromFile);
			updateUrlString = s.nextLine().trim();
			appendLine("Found local update URL: " + updateUrlString);

		} catch (FileNotFoundException e) {
			versionFromFile = DEFAULT_VERSION;
			updateUrlString = "-Insert Link to Update.txt-";

			appendLine(DEFAULT_FILE_NAME + " missing, a new one was created.");
			saveToTextFile(filename);

		} catch (NoSuchElementException e) {
			appendLine("Error: " + DEFAULT_FILE_NAME + " was missing data!");
			if (versionFromFile == null)
				greatestVersionFromServer = "-Insert Version Here-";
			if (updateUrlString == null)
				updateUrlString = "-Insert Link to Update.txt-";
			saveToTextFile(filename);
			success = false;
			// If the file is missing stuff, then exit
			closeGUI();

		} catch (Exception e) {
			if (displayErrorMessages)
				JOptionPane.showMessageDialog(null, "File read error", "Error",
						JOptionPane.ERROR_MESSAGE);
			appendLine(e.getMessage());
			e.printStackTrace();
			success = false;
			// If we can't read the file, we'll just exit
			closeGUI();
		}

		if (s != null) {
			s.close();
		}
	}

	private void saveToTextFile(String filename) {
		PrintWriter out = null;
		try {
			out = new PrintWriter(new BufferedWriter(new FileWriter(filename)));
			out.println(greatestVersionFromServer);
			out.println(updateUrlString);

		} catch (IOException e) {
			if (displayErrorMessages)
				JOptionPane.showMessageDialog(null, "File write error", "Error", JOptionPane.ERROR_MESSAGE);
			appendLine(e.getMessage());
			success = false;
		}

		if (out != null) {
			out.close();
		}
	}

	private void saveConsoleLog(String filename) {
		try {
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(filename)));

			out.print(statusString);
			out.close();
		} catch (IOException ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(null, "File write error", "Error",
					JOptionPane.ERROR_MESSAGE);
			success = false;
		}
	}

	/**
	 * Checks the update.txt file for Versions, and if it finds versions that
	 * are out of date, then it adds them to the lists to update!
	 * 
	 * Modifies: greatestVersionFromServer - When it's through, this will represent the latest version released by the server!
	 * 			 numOfUpdates - The number of updates the program has to install ArrayList<String> versions
	 * 			 ArrayList<String> downloadUrls ArrayList<String> fileNames
	 */
	private void getVersionsFromServer() {
		appendLine("Obtaining version from server.");
		numOfUpdates = 0;
		greatestVersionFromServer = DEFAULT_VERSION;

		BufferedReader in = null;
		try {
			in = new BufferedReader(new InputStreamReader(updateUrl.openStream()));
			appendLine("Recieved server version reply.");
			
			String line;
			while ((line = in.readLine()) != null) {
				// Splits the string into parts based on semi-colons, and
				// creates empty strings if there exists ;;
				String[] updateParts = line.replace(" ", "").split(";", -1);
				if (updateParts.length == 3) {
					if (compareVersion(updateParts[0], greatestVersionFromServer) == GREATER) {
						greatestVersionFromServer = updateParts[0];
						if (compareVersion(updateParts[0], versionFromFile) == GREATER) {
							//Turn the GUI on!
							setVisible(true);
	
							appendLine("Found Update: " + updateParts[0]);
							
							versions.add(updateParts[0]);
							downloadUrls.add(updateParts[1]);
							fileNames.add(updateParts[2]);
							
							numOfUpdates++;
						}

						
					} else if (compareVersion(updateParts[0],
							greatestVersionFromServer) == OUT_OF_DATE) {
						appendLine("Update.txt doesn't look right.");
						appendLine("Updates out of order.  Contact your Neighboorhood Server Admin" + System.lineSeparator());
						appendLine("Ignoring all updates and ignoring check.");
						success = false;
						greatestVersionFromServer = versionFromFile;
						return;
					}
				} else {
					appendLine("Update.txt doesn't look right.");
					appendLine("Expected: Version; DownloadURL; FileName" + System.lineSeparator());
					appendLine("Ignoring all updates and ignoring check.");
					success = false;
					greatestVersionFromServer = versionFromFile;
					return;
				}
			}
		} catch (Exception e) {
			appendLine("Couldn't recieve response from server, ignoring updates");
			appendLine(e.getMessage());
			greatestVersionFromServer = versionFromFile;
			success = false;
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					appendLine("We can't seem to close the download properly");
					appendLine(e.getMessage());
					success = false;
				}
			}
		}
	}

	/**
	 * Compares two version Strings using their hashcodes. Longer Versions
	 * (Parts separated by periods) are considered "more up to date"
	 * 1.1.0 is greater than 1.0.0
	 * 1.1.0 is greater than 1.0.999
	 * a.0.0 is greater than 1.1.1
	 * 9.0 is greater than 1.0.0
	 * 1.0.0 is greater than 1.0
	 * 
	 * @param vf First Version String
	 * @param vs  Second Version String
	 * @return Whether the first version is greater, than the second version,
	 *         equal to, or out of date.
	 */
	private int compareVersion(String vf, String vs) {
		// Split by periods, needed escape sequence \\.
		String[] vfParts = vf.split("\\.");
		String[] vsParts = vs.split("\\.");

		// Compare all parts with each other, starting with the leftmost parts
		for (int i = 0; i < Math.min(vfParts.length, vsParts.length); i++) {
			// Compares the two parts using their hashcodes.
			int result = vfParts[i].hashCode() - vsParts[i].hashCode();
			if (result > 0) {
				return GREATER;
			} else if (result < 0) {
				return OUT_OF_DATE;
			}
		}

		if (vfParts.length > vsParts.length) {
			/*
			 * Version number from the file was "longer" (More numbers separated
			 * by a period) than the version from the server. Assume the file is
			 * messed up, but somehow "more up to date"
			 */
			return GREATER;
		} else if (vfParts.length < vsParts.length) {
			/*
			 * Version number from server was "longer" (More numbers separated
			 * by a period) than the version from the text file. Assume the
			 * server is up to date.
			 */
			return OUT_OF_DATE;
		}

		return EQUAL;
	}

	/**
	 * Tries to update the Installation from a downloaded 7z archive.
	 * 
	 * 1.) Download the 7z archive to a -filename- 2.) Extract the archive at
	 * the running directory 3.) Delete contents from .files.blacklist 4.) Try
	 * to delete .files.blacklist 5.) Try to delete -filename-
	 */
	private void updateDSMinecraftInstallation() {
		if (success) {
			appendLine("Updating from version: " + (versionFromFile.equals("")? "null" : versionFromFile));
			for (int i = 0; i < numOfUpdates; i++) {

				appendLine("Starting Download: " + fileNames.get(i));
				try {
					appendLine("Downloading Update: Please Wait...");
					downloadFromURL(new URL(downloadUrls.get(i)), fileNames.get(i));
				} catch (IOException e) {
					appendLine("File downloading Error!");
					appendLine(e.getMessage());
					greatestVersionFromServer = versionFromFile;
					success = false;
					return;
				}

				appendLine("Installing: " + versions.get(i));

				appendLine("Extracting " + fileNames.get(i) + " to " + System.getProperty("user.dir"));
				downloadProgress.setString("Extracting " + fileNames.get(i) + " to local directory");
				try {
					statusString = ExtractItemsStandard.extract(fileNames.get(i), statusString);
				} catch (Exception e) {
					appendLine("Encountered an Error while Unzipping Files");
					appendLine(e.getMessage());
				}

				appendLine("Opening: " + DEFAULT_BLACKLIST_NAME);

				BufferedReader in = null;
				try {
					in = new BufferedReader(new FileReader(
							DEFAULT_BLACKLIST_NAME));
					String line = in.readLine();
					while (line != null) {
						// Works on windows installations
						line = line.replaceAll("/", Matcher.quoteReplacement(File.separator));

						// Next line doesn't work... but may be necessary for Linux distro's
						// line = line.replaceAll("\\", Matcher.quoteReplacement(File.separator));

						appendLine("Removing " + line);
						Files.deleteIfExists(Paths.get(line));
						line = in.readLine();
					}

					if (in != null) {
						in.close();
					}
				} catch (IOException e) {
					appendLine("Warning: " + DEFAULT_BLACKLIST_NAME
							+ " doesn't exist, or couldn't be opened properly.");
					appendLine(e.getMessage());
				} finally {
					if (in != null) {
						try {
							in.close();
						} catch (IOException e) {
							appendLine("Error Closing "
									+ DEFAULT_BLACKLIST_NAME);
							appendLine(e.getMessage());
						}
					}
				}

				appendLine("Removing " + DEFAULT_BLACKLIST_NAME);
				try {
					Files.deleteIfExists(Paths.get(DEFAULT_BLACKLIST_NAME));
				} catch (IOException e) {
					appendLine("Warning: " + DEFAULT_BLACKLIST_NAME
							+ " isn't behaving properly.");
					appendLine(e.getMessage());
				}

				appendLine("Removing " + fileNames.get(i));
				try {
					Files.deleteIfExists(Paths.get(fileNames.get(i)));
				} catch (IOException e) {
					appendLine("Warning: " + fileNames.get(i)
							+ " isn't behaving properly.");
					appendLine(e.getMessage());
				}

				versionFromFile = versions.get(i);
			}
			appendLine("Updated to version: " + versionFromFile);
		} else {
			appendLine("An error occured.  Skipping Updates.");
		}
	}

	/**
	 * Downloads a file hosted at a given URL, and stores it as a local file (in
	 * the running directory) with the given name
	 * 
	 * @param url - Where the file is hosted
	 * @param localFilename - What to save the file as
	 * @throws IOException - Errors that occur using the FileOutputStream
	 */
	void downloadFromURL(URL url, String localFilename) throws IOException {
		InputStream i = null;
		FileOutputStream f = null;

		try {
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.connect();

			i = connection.getInputStream(); // get connection inputstream
			f = new FileOutputStream(localFilename); // open outputstream to local file
			double downloaded = 0.0;
			int fileSize = connection.getContentLength();

			//downloadProgress.setMaximum(fileSize);
			downloadProgress.setIndeterminate(false);

			byte[] buffer = new byte[BUFFER_SIZE];
			int len;

			// while we have available data, continue downloading and storing to
			// local file
			while ((len = i.read(buffer)) > 0) {
				f.write(buffer, 0, len);
				if (fileSize > 0) {
					downloaded += len;
					// System.out.println(downloaded + "/" + fileSize + " kB");
					downloadProgress.setValue((int) (100*downloaded/fileSize));
				}
			} 
		} catch (IOException e) {
			throw new IOException("The URL could not be instantiated: " + url.toString());
		} finally {
			if (f != null) {
				f.close();
			}
			if (i != null) {
				i.close();
			}
			downloadProgress.setIndeterminate(true);
		}
	}
	
	/**
	 * Appends the given string to the statusLabel JLabel
	 * 
	 * @param str String to append
	 */
	public void appendLine(String str) {
		if (str != null) {
			System.out.println(str);
			statusString += (str + System.lineSeparator());
			downloadProgress.setString(str);//Instantiate this first
		}
	}
	
	private class DrawBackground extends JPanel {
		/* Makes that stupid dependency warning go away... */
		private static final long serialVersionUID = 1L;

		URL imageURL;
		
		public DrawBackground() {
			setBackground(new Color(0,0,0,0));
			setOpaque(false);
			background = null;
			logo = null;
			
			imageURL = this.getClass().getClassLoader().getResource(backgroundImageLocation);
	        if  (imageURL != null) {
	            background = new ImageIcon(imageURL);
	        } else {
	        	appendLine("Warning: Internal Background file missing at:");
	        	appendLine(backgroundImageLocation);
	        }
	        
	        imageURL = this.getClass().getClassLoader().getResource(logoImageLocation);
	        if  (imageURL != null) {
	            logo = new ImageIcon(imageURL);
	        } else {
	        	appendLine("Warning: Internal Icon file missing at:");
	        }
		}
		
		@Override
		public void paint(Graphics g) {
			if (background != null)
				background.paintIcon(this, g, 23, 11);
			if (logo != null)
				logo.paintIcon(this, g, 0, 0);
		}
	}
}
