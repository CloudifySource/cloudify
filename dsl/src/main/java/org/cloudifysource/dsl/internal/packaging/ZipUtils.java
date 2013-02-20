/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.cloudifysource.dsl.internal.packaging;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Enumeration;
import java.util.Stack;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/*********
 * Static utility methods for using zip and unzip.
 * 
 * @author barakme
 * @since 1.0
 * 
 */
public final class ZipUtils {

	private static final int BUFFER_SIZE = 1024;

	private ZipUtils() {

	}

	/***********
	 * Zips a single source file into the given file.
	 * 
	 * @param sourceFile the file to zip.
	 * @param zipfile the zip file to create.
	 * @throws IOException in case of an error.
	 */
	public static void zipSingleFile(final File sourceFile, final File zipfile)
			throws IOException {

		if (!sourceFile.exists()) {
			throw new FileNotFoundException("Could not find: " + sourceFile);
		}

		if (!sourceFile.isFile()) {
			throw new IllegalArgumentException(sourceFile + " is not a file!");
		}

		final File toZip = new File(zipfile, "");

		toZip.setWritable(true);
		final ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(toZip));

		try {
			final String name = sourceFile.getName();
			zout.putNextEntry(new ZipEntry(name));
			copy(sourceFile, zout);
			zout.closeEntry();

		} finally {
			zout.close();
		}
	}

	/***********
	 * Zips a directory into the given file.
	 * 
	 * @param directory the directory to zip.
	 * @param zipfile the zip file to create.
	 * @throws IOException in case of an error.
	 */
	public static void zip(final File directory, final File zipfile)
			throws IOException {
		final URI base = directory.toURI();
		final File toZip = new File(zipfile, "");
		toZip.setWritable(true);
		final Stack<File> stack = new Stack<File>();
		stack.push(directory);
		final OutputStream out = new FileOutputStream(toZip);
		Closeable res = out;

		try {
			final ZipOutputStream zout = new ZipOutputStream(out);
			res = zout;
			while (!stack.isEmpty()) {
				final File currentDirectory = stack.pop();
				for (final File kid : currentDirectory.listFiles()) {
					String name = base.relativize(kid.toURI()).getPath();
					if (kid.isDirectory()) {
						stack.push(kid);
						name = name.endsWith("/") ? name : name + "/";
						zout.putNextEntry(new ZipEntry(name));
					} else {
						zout.putNextEntry(new ZipEntry(name));
						copy(kid, zout);
						zout.closeEntry();
					}
				}
			}
		} finally {
			res.close();
		}
	}

	/*************
	 * Unzip the given zip file into the specified directory.
	 * 
	 * @param zipfile the zip file.
	 * @param directory the target directory.
	 * @throws IOException .
	 */
	public static void unzip(final File zipfile, final File directory)
			throws IOException {
		final ZipFile zfile = new ZipFile(zipfile);
		final Enumeration<? extends ZipEntry> entries = zfile.entries();
		while (entries.hasMoreElements()) {
			final ZipEntry entry = entries.nextElement();
			final File file = new File(directory, entry.getName());
			if (entry.isDirectory()) {
				final boolean mkdirs = file.mkdirs();
				if (!mkdirs) {
					zfile.close();
					throw new IllegalStateException("cant create dir" + file.getAbsolutePath());
				}
			} else {
				if (!file.getParentFile().exists()) {
					final boolean mkdirs = file.getParentFile().mkdirs();
					if (!mkdirs) {
						zfile.close();
						throw new IllegalStateException("cant create dir" + file.getParentFile().getAbsolutePath());
					}
				}
				final InputStream in = zfile.getInputStream(entry);
				try {
					copy(in, file);
				} finally {
					in.close();
				}
			}
		}
	}

	/***************
	 * Unzips a specific entry from a zip file to a temporary directory. 
	 * 
	 * @param zipfile the zip file.
	 * @param entryName the entry to unzip.
	 * @param fileName the file name of the created file, which will be placed in a directory.
	 * @return the unzipped file matching the entry, or null if the entry name was not found.
	 * @throws IOException if an error occured.
	 * 
	 */
	public static File unzipEntry(final File zipfile, final String entryName, final String fileName)
			throws IOException {
		final ZipFile zfile = new ZipFile(zipfile);
		final ZipEntry entry = zfile.getEntry(entryName);
		if (entry == null) {
			return null;
		}

		final InputStream in = zfile.getInputStream(entry);
		final File entryDirectory = File.createTempFile("cloudConfiguFile", ".tmp");
		entryDirectory.delete();
		entryDirectory.mkdirs();
		final File entryFile = new File(entryDirectory, fileName);

		entryFile.deleteOnExit();
		entryDirectory.deleteOnExit();

		try {
			copy(in, entryFile);
		} finally {
			in.close();
		}
		return entryFile;

	}

	private static void copy(final InputStream in, final OutputStream out)
			throws IOException {
		final byte[] buffer = new byte[BUFFER_SIZE];
		while (true) {
			final int readCount = in.read(buffer);
			if (readCount < 0) {
				break;
			}
			out.write(buffer, 0, readCount);
		}
	}

	private static void copy(final File file, final OutputStream out)
			throws IOException {
		final InputStream in = new FileInputStream(file);
		try {
			copy(in, out);
		} finally {
			in.close();
		}
	}

	private static void copy(final InputStream in, final File file)
			throws IOException {
		final OutputStream out = new FileOutputStream(file);
		try {
			copy(in, out);
		} finally {
			out.close();
		}
	}
}
