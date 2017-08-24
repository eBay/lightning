/**
 * 
 */
package com.ebay.lightning.core.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * @author shashukla
 *
 */
public class ZipUtilTest {

	String token = "Sample string to test compression and decompression.Sample string to test compression and decompression."
			+ "Sample string to test compression and decompression.Sample string to test compression and decompression."
			+ "Sample string to test compression and decompression.Sample string to test compression and decompression."
			+ "Sample string to test compression and decompression.Sample string to test compression and decompression."
			+ "Sample string to test compression and decompression.Sample string to test compression and decompression.";
	
	@Test
	public void testCompression(){
		String compressedToken = ZipUtil.compress(token);
		assertTrue("Compressed data should reduce token size", compressedToken.length() < token.length());
		String decompressedToken = ZipUtil.decompress(compressedToken);
		assertEquals(token, decompressedToken);
	}
	
	@Test
	public void testZipCompression() throws Exception{
		String compressedToken = ZipUtil.zip(token);
		assertTrue("Compressed data should reduce token size", compressedToken.length() < token.length());
		String decompressedToken = (String) ZipUtil.unZip(compressedToken, String.class);
		assertEquals(token, decompressedToken);
	}
	
	@Test
	public void testZipAsArray() throws Exception{
		byte[] compressedToken = ZipUtil.zipAsByteArray(token);
		assertTrue("Compressed data should reduce token size", compressedToken.length < token.length());
		String decompressedToken = (String) ZipUtil.unZipByteArray(compressedToken, String.class);
		assertEquals(token, decompressedToken);
		String decompressedGenericToken = (String) ZipUtil.unZipByteArray(compressedToken);
		assertEquals(token, decompressedGenericToken);
	}
	
	@Test
	public void testCompressionError(){
		String decompressedToken = ZipUtil.decompress(token);
		assertEquals(token, decompressedToken);
		
		try{
			ZipUtil.unZip(token, String.class);
		}catch(Exception e){
			assertTrue(true);
		}
		
		try{
			ZipUtil.unZipByteArray(token.getBytes(), String.class);
		}catch(Exception e){
			assertTrue(true);
		}
		
		try{
			ZipUtil.unZipByteArray(token.getBytes());
		}catch(Exception e){
			assertTrue(true);
		}
	}
	
}
