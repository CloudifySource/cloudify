/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/
package org.cloudifysource.dsl.internal.tools.download;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

/**
 * This class is used to validate a file's checksum against a validation hash file containing the valid checksum.
 * Checksum algorithms include md5, sha1, sha256, sha384 and sha512.
 * 
 * @author adaml
 * @since 2.6.0
 *
 */
public class ChecksumVerifier {

	/**
	 * checksum constant enum for maintaining the digest message algorithm id.
	 * see {@link java.security.MessageDigest}. the enum exposes a static getAlgorithm
	 * method used to determine hashing id according to it's file extension. 
	 *  
	 */
	public enum ChecksumAlgorithm {
		/**
		 * md5 hashing id.
		 */
        MD5("MD5"),
        /**
         * sha-1 hashing id.
         */
        SHA1("SHA-1"),
        /**
         * sha-256 hashing id.
         */
        SHA256("SHA-256"),
        /**
         * sha-384 hashing id.
         */
        SHA384("SHA-384"),
        /**
         * sha-512 hashing id.
         */
        SHA512("SHA-512");
        
        private String algorithm;

        ChecksumAlgorithm(final String algorithm) {
            this.algorithm = algorithm;
        }

        public String getValue() {
            return algorithm;
        }
        
        /**
         * returns the hashing algorithm's hashing id {@link java.security.MessageDigest} 
         * according to file extension. the file extension should indicate the message hashing,
         * for example: 'md5'/'sha1'. 
         * @param ext
         * 		the file extension.
         * @return
         * 		returns the MessageDigest hashing id if found else returns null. 
         */
        public static String toAlgorithm(final String ext) {
        	for (ChecksumAlgorithm cs : ChecksumAlgorithm.values()) {
				if (cs.getValue().replaceAll("-", "").equalsIgnoreCase(ext)) {
					return cs.getValue();
				}
			}
        	return null;
        }
        
        public static List<String> names() {
        	List<String> algoNames = new ArrayList<String>();
        	for (ChecksumAlgorithm algo : ChecksumAlgorithm.values()) {
				algoNames.add(algo.name());
			}
        	return algoNames;
        }
    }
	
	private static final Logger logger = Logger
			.getLogger(ChecksumVerifier.class.getName());
	
	private File hashFile;
	
	private File file;
	
	private MessageFormat format = new MessageFormat("{0} *{1}");
	
	public File getHashFile() {
		return hashFile;
	}

	public void setHashFile(final File hashFile) {
		this.hashFile = hashFile;
	}

	public File getFile() {
		return file;
	}

	public void setFile(final File file) {
		this.file = file;
	}
	
	public void setFormat(final MessageFormat format) {
		this.format = format;
	}

	public MessageFormat getFormat() {
		return this.format;
	}

	/**
	 * evaluates the file checksum against the given hash file.
	 * @return
	 * 		true if checksum matches else returns false.
	 * @throws ChecksumVerifierException
	 * 		in case of an exception during the evaluation process.
	 */
	public boolean evaluate() 
			throws ChecksumVerifierException {
		
		final String resourceHash = calculateFileDigest();
		String checksum;
		logger.log(Level.FINE, "Checksum result for " + this.file.getPath() + " is " + resourceHash);
		checksum = readChecksum(this.hashFile);
		if (StringUtils.isEmpty(checksum)) {
			throw new ChecksumVerifierException("hash file does not contain any data");
		}
		if (StringUtils.isEmpty(resourceHash)) {
			throw new ChecksumVerifierException("resource hash calculation failed. " 
					+ " This should not happen");
		}
		if (!checksum.equals(resourceHash)) {
			throw new ChecksumVerifierException("checksum validation failed. File hash is " + resourceHash 
					+ " while checksum is " + checksum);
		}
		logger.info("File verification completed successfully.");
		return true;
	}
	
	/**
	 * calculates the file hash. 
	 * @return
	 * 		the file hash.
	 * @throws ChecksumVerifierException
	 * 		if calculation fails.
	 * 			
	 */
	public String calculateFileDigest() throws ChecksumVerifierException {
		
		final String hashFileName = this.hashFile.getName();
		final String hashFileExt = getFileExtention(hashFileName);
		final String checksumAlgorithm = ChecksumAlgorithm.toAlgorithm(hashFileExt);
		if (checksumAlgorithm == null) {
			throw new ChecksumVerifierException("Validation checksum method " + hashFileExt + " is not supported."
							+ " Hash file extention should match one of the following values: "
							+ ChecksumAlgorithm.names());
		}
		MessageDigest messageDigest;
		try {
			messageDigest = MessageDigest.getInstance(checksumAlgorithm);
		} catch (NoSuchAlgorithmException e) {
			throw new ChecksumVerifierException("Algorithm " + checksumAlgorithm 
					+ " does not exist or is not supported.", e);
		}
		if (messageDigest == null) {
			throw new ChecksumVerifierException("Unable to create Message Digest for algorithm " + checksumAlgorithm);
		}
		
		final byte[] buffer = new byte[(int) this.file.length()];
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(this.file);
			fis.read(buffer);
		} catch (FileNotFoundException e) {
			logger.warning("Could not find file to digest.");
			throw new IllegalStateException("Resource was not found.", e);
		} catch (IOException e) {
			throw new ChecksumVerifierException("Failed calculating file hash.", e);
		} finally {
			IOUtils.closeQuietly(fis);
		}
		
		messageDigest.reset();
		messageDigest.update(buffer);
		final byte[] digest = messageDigest.digest();
		return Hex.encodeHexString(digest);
	}
	
	private String getFileExtention(final String resourceName) {
		String extension = "";
		int i = resourceName.lastIndexOf('.');
		if (i > 0) {
		    extension = resourceName.substring(i + 1);
		}
		return extension;
	}
	
    private String readChecksum(final File checksumFile) 
    			throws ChecksumVerifierException {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(checksumFile));
            logger.fine("reading checksum from file " + checksumFile);
            final Object[] result = format.parse(br.readLine());
            if (result == null || result.length == 0 || result[0] == null) {
                throw new ChecksumVerifierException("a checksum hash could not be found.");
            }
            return (String) result[0];
        } catch (IOException e) {
            throw new ChecksumVerifierException("Couldn't read checksum file " + checksumFile, e);
        } catch (ParseException e) {
            throw new ChecksumVerifierException("Couldn't parse checksum file " + checksumFile, e);
        } finally {
        	IOUtils.closeQuietly(br);
        }
    }
}
