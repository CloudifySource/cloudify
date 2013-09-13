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
 ******************************************************************************/
package org.cloudifysource.esc.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

/**
 * Class utility to create TAR.GZ archives.
 * 
 */
public final class TarGzUtils {

	private static final Logger LOGGER = Logger.getLogger(TarGzUtils.class.getName());

	private static final String DEFAULT_PREFIX = "cloudFolder";
	private static final int BUFFER = 2048;

	private TarGzUtils() {
	}

	/**
	 * Create a temporary tar gz file.
	 * 
	 * @return The temporary file
	 * @throws IOException
	 *             If the file cannot be created.
	 */
	private static File createTempTarGzFile() throws IOException {
		final File archiveFile = File.createTempFile(DEFAULT_PREFIX, ".tar.gz");
		archiveFile.deleteOnExit();
		if (LOGGER.isLoggable(Level.FINE)) {
			System.out.println(archiveFile);
			LOGGER.finest("Created tar.gz file: " + archiveFile);
		}

		return archiveFile;
	}

	/**
	 * Create a tar.gz file.
	 * 
	 * @param sourcePaths
	 *            Folders or files to add in the archive.
	 * @param addRoot
	 *            When <code>sourcePath</code> is a folder. if true, it will add the folder in the archive.<br/>
	 *            <i>i.e: if sourcepath=/tmp/folderToInclude, archive.tar.gz will include the folder
	 *            <b>folderToInclude</b> in the archive.</i>
	 * @return The created archive.
	 * @throws IOException
	 *             If the archive cannot be create.
	 */
	public static File createTarGz(final String sourcePaths, final boolean addRoot)
			throws IOException {
		return createTarGz(new String[] { sourcePaths }, "", addRoot);
	}

	/**
	 * Create a tar.gz file.
	 * 
	 * @param sourcePaths
	 *            Folders or files to add in the archive.
	 * @param addRoot
	 *            When <code>sourcePath</code> is a folder. if true, it will add the folder in the archive.<br/>
	 *            <i>i.e: if sourcepath=/tmp/folderToInclude, archive.tar.gz will include the folder
	 *            <b>folderToInclude</b> in the archive.</i>
	 * @return The created archive.
	 * @throws IOException
	 *             If the archive cannot be create.
	 */
	public static File createTarGz(final String[] sourcePaths, final boolean addRoot)
			throws IOException {
		return createTarGz(sourcePaths, "", addRoot);
	}

	/**
	 * Create a tar.gz file.
	 * 
	 * @param sourcePaths
	 *            Folders or files to add in the archive.
	 * @param base
	 *            The name to be use in the archive.
	 * @param addRoot
	 *            When <code>sourcePath</code> is a folder. if true, it will add the folder in the archive.<br/>
	 *            <i>i.e: if sourcepath=/tmp/folderToInclude, archive.tar.gz will include the folder
	 *            <b>folderToInclude</b> in the archive.</i>
	 * @return The created archive.
	 * @throws IOException
	 *             If the archive cannot be create.
	 */
	public static File createTarGz(final String[] sourcePaths, final String base,
			final boolean addRoot) throws IOException {
		File tarGzFile = createTempTarGzFile();

		if (!FilenameUtils.getExtension(tarGzFile.getName().toLowerCase()).equals("gz")) {
			throw new IllegalArgumentException("Expecting tar.gz file: " + tarGzFile.getAbsolutePath());
		}

		FileOutputStream fOut = null;
		BufferedOutputStream bOut = null;
		GzipCompressorOutputStream gzOut = null;
		TarArchiveOutputStream tOut = null;
		try {
			fOut = new FileOutputStream(tarGzFile);
			bOut = new BufferedOutputStream(fOut);
			gzOut = new GzipCompressorOutputStream(bOut);
			tOut = new TarArchiveOutputStream(gzOut);
			for (String path : sourcePaths) {
				addFileToTarGz(tOut, path, base, addRoot);
			}
		} finally {
			if (tOut != null) {
				tOut.close();
			}
			if (gzOut != null) {
				gzOut.close();
			}
			if (bOut != null) {
				bOut.close();
			}
			if (fOut != null) {
				fOut.close();
			}
		}

		return tarGzFile;
	}

	private static void addFileToTarGz(final TarArchiveOutputStream tOut, final String path, final String base,
			final boolean addRoot)
			throws IOException {
		File f = new File(path);
		String entryName = base + f.getName();

		if (f.isFile()) {
			TarArchiveEntry tarEntry = new TarArchiveEntry(f, entryName);
			tOut.putArchiveEntry(tarEntry);
			IOUtils.copy(new FileInputStream(f), tOut);
			tOut.closeArchiveEntry();
		} else {
			if (addRoot) {
				TarArchiveEntry tarEntry = new TarArchiveEntry(f, entryName);
				tOut.putArchiveEntry(tarEntry);
				tOut.closeArchiveEntry();
			}
			File[] children = f.listFiles();
			if (children != null) {
				for (File child : children) {
					if (addRoot) {
						addFileToTarGz(tOut, child.getAbsolutePath(), entryName + "/", true);
					} else {
						addFileToTarGz(tOut, child.getAbsolutePath(), "", true);
					}
				}
			}
		}
	}

	/**
	 * Extract a tar.gz file.
	 * 
	 * @param source
	 *            The file to extract from.
	 * @param destination
	 *            The destination folder.
	 * @throws IOException
	 *             An error occured during the extraction.
	 */
	public static void extract(final File source, final String destination) throws IOException {

		LOGGER.fine(String.format("Extracting %s to %s", source.getName(), destination));

		if (!FilenameUtils.getExtension(source.getName().toLowerCase()).equals("gz")) {
			throw new IllegalArgumentException("Expecting tar.gz file: " + source.getAbsolutePath());
		}
		if (!new File(destination).isDirectory()) {
			throw new IllegalArgumentException("Destination should be a folder: " + destination);
		}

		/** create a TarArchiveInputStream object. **/
		FileInputStream fin = new FileInputStream(source);
		BufferedInputStream in = new BufferedInputStream(fin);
		GzipCompressorInputStream gzIn = new GzipCompressorInputStream(in);
		TarArchiveInputStream tarIn = new TarArchiveInputStream(gzIn);

		TarArchiveEntry entry = null;

		/** Read the tar entries using the getNextEntry method **/
		while ((entry = (TarArchiveEntry) tarIn.getNextEntry()) != null) {

			LOGGER.finer("Extracting: " + entry.getName());

			/** If the entry is a directory, create the directory. **/
			if (entry.isDirectory()) {

				File f = new File(destination, entry.getName());
				f.mkdirs();
			} else {
				int count;
				byte[] data = new byte[BUFFER];
				FileOutputStream fos = new FileOutputStream(new File(destination, entry.getName()));
				BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER);
				while ((count = tarIn.read(data, 0, BUFFER)) != -1) {
					dest.write(data, 0, count);
				}
				dest.close();
			}
		}

		/** Close the input stream **/
		tarIn.close();
	}

}
