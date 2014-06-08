package src;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.sf.sevenzipjbinding.ExtractAskMode;
import net.sf.sevenzipjbinding.ExtractOperationResult;
import net.sf.sevenzipjbinding.IArchiveExtractCallback;
import net.sf.sevenzipjbinding.ISequentialOutStream;
import net.sf.sevenzipjbinding.ISevenZipInArchive;
import net.sf.sevenzipjbinding.PropID;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;

public class ExtractItemsStandard {
	public static class MyExtractCallback implements IArchiveExtractCallback {
		private int hash = 0;
		private int size = 0;
		private int index;
		private ISevenZipInArchive inArchive;
		private String logging;
		private String lastFile = "";

		public MyExtractCallback(ISevenZipInArchive inArchive, String logging) {
			this.inArchive = inArchive;
			this.logging = logging;
		}

		public String getLog() {
			return logging;
		}
		
		public ISequentialOutStream getStream(final int index,
				ExtractAskMode extractAskMode) throws SevenZipException {
			this.index = index;
			if (extractAskMode != ExtractAskMode.EXTRACT) {
				return null;
			}
			return new ISequentialOutStream() {

				public int write(byte[] data) throws SevenZipException {
					FileOutputStream out = null;
					
					try {
						File file = new File((String) inArchive.getProperty(index, PropID.PATH));
						try {
							file.getParentFile().mkdirs();
						} catch (NullPointerException e) {
							// File is in the root directory
						}
						if (lastFile.equals((String) inArchive.getProperty(index, PropID.PATH)))
							appendLine("GOT ONE! " + lastFile);
						out = new FileOutputStream(file, (lastFile.equals((String) inArchive.getProperty(index, PropID.PATH))));
						out.write(data);
						lastFile = (String) inArchive.getProperty(index, PropID.PATH);

					} catch (FileNotFoundException e) {
						appendLine(e.getMessage());
					} catch (IOException e) {
						appendLine(e.getMessage());
					} finally {
						if (out != null)
							try {
								out.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
					}

					hash ^= Arrays.hashCode(data);
					size += data.length;
					return data.length; // Return amount of proceed data
				}
			};
		}

		public void prepareOperation(ExtractAskMode extractAskMode)
				throws SevenZipException {
		}

		public void setOperationResult(
				ExtractOperationResult extractOperationResult)
				throws SevenZipException {
			if (extractOperationResult != ExtractOperationResult.OK) {
				appendLine("Extraction error");
				
			} else {
				appendLine(String.format("%9X | %10s | %s", hash, size,
						inArchive.getProperty(index, PropID.PATH)));
				hash = 0;
				size = 0;
			}
		}

		public void setCompleted(long completeValue) throws SevenZipException {
		}

		public void setTotal(long total) throws SevenZipException {
		}
		
		public void appendLine(String str) {
			System.out.println(str);
			logging += (str + System.lineSeparator());
		}
		

	}

	public static String extract(String filename, String log) {
		RandomAccessFile randomAccessFile = null;
		ISevenZipInArchive inArchive = null;
		
		MyExtractCallback m = null;
		
		try {
			randomAccessFile = new RandomAccessFile(filename, "r");
			inArchive = SevenZip.openInArchive(null, // autodetect archive type
					new RandomAccessFileInStream(randomAccessFile));

			log += "   Hash   |    Size    | Filename" + System.lineSeparator();
			log += "----------+------------+---------" + System.lineSeparator();
			System.out.println("   Hash   |    Size    | Filename");
			System.out.println("----------+------------+---------");

			int count = inArchive.getNumberOfItems();
			List<Integer> itemsToExtract = new ArrayList<Integer>();
			for (int i = 0; i < count; i++) {
				if (!((Boolean) inArchive.getProperty(i, PropID.IS_FOLDER))
						.booleanValue()) {
					itemsToExtract.add(Integer.valueOf(i));
				}
			}
			int[] items = new int[itemsToExtract.size()];
			int i = 0;
			for (Integer integer : itemsToExtract) {
				items[i++] = integer.intValue();
			}
			
			m = new MyExtractCallback(inArchive, log);
			inArchive.extract(items, false, m);
					

		} catch (Exception e) {
			System.err.println("Error occurs: " + e);
			e.printStackTrace();
			System.exit(1);
		} finally {
			if (inArchive != null) {
				try {
					inArchive.close();
				} catch (SevenZipException e) {
					System.err.println("Error closing archive: " + e);
				}
			}
			if (randomAccessFile != null) {
				try {
					randomAccessFile.close();
				} catch (IOException e) {
					System.err.println("Error closing file: " + e);
				}
			}
		}
		return m.getLog();
	}
}