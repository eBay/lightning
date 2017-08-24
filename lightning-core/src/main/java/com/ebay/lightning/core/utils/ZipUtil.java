package com.ebay.lightning.core.utils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.log4j.Logger;

/**
 * Helper class to compress and extract String and Object data.
 * 
 * @author shashukla
 *
 */
public class ZipUtil {
	/**
	 * 
	 */
	private static final String ISO_8859_1 = "ISO-8859-1";
	
	private static final Logger log = Logger.getLogger(ZipUtil.class);

	/**
	 * Compresses the data
	 * @param str the data to compress
	 * @return compressed data
	 */
	@Deprecated
	public static String compress(String str) {
		if (str == null || str.length() == 0) {
			return str;
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		GZIPOutputStream gzip;
		try {
			gzip = new GZIPOutputStream(out);
			gzip.write(str.getBytes());
			gzip.close();

		} catch (IOException e) {
			log.error("Error writing to gzip stream: ", e);
		}
		try {
			return out.toString(ISO_8859_1);
		} catch (UnsupportedEncodingException e) {
			log.error("Unsupported Encoding: ", e);
		}
		return str;
	}

	/**
	 * Extract the compressed data
	 * @param str the compressed data
	 * @return extracted data
	 */
	@Deprecated
	public static String decompress(String str) {
		if (str == null || str.length() == 0) {
			return str;
		}
		String outStr = "";

		try {
			GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(str.getBytes(ISO_8859_1)));
			BufferedReader bf = new BufferedReader(new InputStreamReader(gis, ISO_8859_1));
			String line;
			while ((line = bf.readLine()) != null) {
				outStr += line;
			}
		} catch (IOException io) {
			if ("Not in GZIP format".contains(io.getMessage())) {
				return str;
			}
		}

		return outStr;
	}

	/**
	 * Compress the object to String.
	 * @param obj the object to compress
	 * @return object compressed as string
	 * @throws IOException when the compression fails
	 */
	public static String zip(Object obj) throws IOException {
		ByteArrayOutputStream baos = getZippedByteOutputStream(obj);

		return baos.toString(ISO_8859_1);
	}

	/**
	 * Compress the object to byte stream.
	 * @param obj the object to compress
	 * @return compressed byte stream
	 * @throws IOException when the compression fails
	 */
	private static ByteArrayOutputStream getZippedByteOutputStream(Object obj) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		GZIPOutputStream gzipOut = new GZIPOutputStream(baos);
		ObjectOutputStream objectOut = null;
		
		try {
			objectOut = new ObjectOutputStream(gzipOut);
			objectOut.writeObject(obj);
		} catch (Exception e) {
			log.error("Error while getting zipped outputstream: " + e.getMessage());
		} finally {
			closeOutputStream(objectOut);
		}
		
		return baos;
	}

	/**
	 * Extract the object from the compressed string
	 * @param zippedOutputData compressed data
	 * @param classname the class name of the extracted object
	 * @return the object extracted
	 * @throws Exception when the data extraction fails
	 */
	public static Object unZip(String zippedOutputData, Class<?> classname) throws Exception {
		Object myObj = null;
		ByteArrayInputStream bais = new ByteArrayInputStream(zippedOutputData.getBytes(ISO_8859_1));
		GZIPInputStream gzipIn = new GZIPInputStream(bais);
		ObjectInputStream objectIn = new ObjectInputStream(gzipIn);

		try {
			myObj = objectIn.readObject();
		} catch (Exception e) {
			log.error("Error unZip: " + e.getMessage());
		} finally {
			closeInputStream(objectIn);
		}

		return classname.cast(myObj);
	}

	/**
	 * Compress the object to byte array.
	 * @param obj the object to compress
	 * @return compressed byte array
	 * @throws IOException when the compression fails
	 */
	public static byte[] zipAsByteArray(Object obj) throws IOException {
		ByteArrayOutputStream baos = getZippedByteOutputStream(obj);
		return baos.toByteArray();
	}

	/**
	 * Extract the object from the byte array
	 * @param zippedOutputData the byte array
	 * @param classname the class name of the extracted object
	 * @return the object extracted
	 * @throws Exception when the data extraction fails
	 */
	public static Object unZipByteArray(byte[] zippedOutputData, Class<?> classname) throws Exception {
		Object myObj = unZipByteArray(zippedOutputData);
		return classname.cast(myObj);
	}
	
	/**
	 * Extract the object from the byte array
	 * @param zippedOutputData the byte array
	 * @return the object extracted
	 * @throws Exception when the data extraction fails
	 */
	public static Object unZipByteArray(byte[] zippedOutputData) throws Exception {
		Object myObj = null;
		ByteArrayInputStream bais = new ByteArrayInputStream(zippedOutputData);
		GZIPInputStream gzipIn = new GZIPInputStream(bais);
		ObjectInputStream objectIn = new ObjectInputStream(gzipIn);
		
		try {
			myObj = objectIn.readObject();
		} catch (Exception e) {
			log.error("Error unZipByteArray: " + e.getMessage());
		} finally {
			closeInputStream(objectIn);
		}
		
		return myObj;
	}
	
	/**
	 * Closes the input stream object
	 * @param objectIn the input stream
	 */
	private static void closeInputStream(ObjectInputStream objectIn){
		if (objectIn != null) {
			try {
				objectIn.close();
			} catch (Exception e) {
				log.error(e.getMessage());
			}
		}
	}
	
	/**
	 * Closes the output stream object
	 * @param objectIn the output stream
	 */
	private static void closeOutputStream(ObjectOutputStream objectOut){
		if (objectOut != null) {
			try {
				objectOut.close();
			} catch (Exception e) {
				log.error(e.getMessage());
			}
		}
	}
}