package com.gigaspaces.cloudify.dsl.internal.packaging;

import java.io.*;
import java.net.URI;
import java.util.Enumeration;
import java.util.Stack;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ZipUtils {

	public static void zip(File directory, File zipfile) throws IOException {
		URI base = directory.toURI();
		File toZip = new File(zipfile, "");
		toZip.setWritable(true);
		Stack<File> stack = new Stack<File>();
		stack.push(directory);
		OutputStream out = new FileOutputStream(toZip);
		Closeable res = out;
		try {
			ZipOutputStream zout = new ZipOutputStream(out);
			res = zout;
			while (!stack.isEmpty()) {
				directory = stack.pop();
				for (File kid : directory.listFiles()) {
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

	public static void unzip(File zipfile, File directory) throws IOException {
		ZipFile zfile = new ZipFile(zipfile);
		Enumeration<? extends ZipEntry> entries = zfile.entries();
		while (entries.hasMoreElements()) {
			ZipEntry entry = entries.nextElement();
			File file = new File(directory, entry.getName());
			if (entry.isDirectory()) {
				boolean mkdirs = file.mkdirs();
				if (!mkdirs) {
					zfile.close();
					throw new IllegalStateException("cant create dir" + file.getAbsolutePath());
				}
			} else {
				if (!file.getParentFile().exists()){
					boolean mkdirs = file.getParentFile().mkdirs();
					if (!mkdirs) {
						zfile.close();
						throw new IllegalStateException("cant create dir" + file.getParentFile().getAbsolutePath());
					}
				}
				InputStream in = zfile.getInputStream(entry);
				try {
					copy(in, file);
				} finally {
					in.close();
				}
			}
		}
	}

	private static void copy(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[1024];
		while (true) {
			int readCount = in.read(buffer);
			if (readCount < 0) {
				break;
			}
			out.write(buffer, 0, readCount);
		}
	}

	private static void copy(File file, OutputStream out) throws IOException {
		InputStream in = new FileInputStream(file);
		try {
			copy(in, out);
		} finally {
			in.close();
		}
	}

	private static void copy(InputStream in, File file) throws IOException {
		OutputStream out = new FileOutputStream(file);
		try {
			copy(in, out);
		} finally {
			out.close();
		}
	}
}
