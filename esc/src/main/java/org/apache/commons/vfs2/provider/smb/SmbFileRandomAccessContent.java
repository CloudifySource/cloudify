/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.vfs2.provider.smb;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.UnknownHostException;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbRandomAccessFile;

import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.provider.AbstractRandomAccessContent;
import org.apache.commons.vfs2.util.RandomAccessMode;

/**
 * CHECKSTYLE:OFF.
 * RandomAccess for smb files
 *  *
 * @author <a href="mailto:imario@apache.org">Mario Ivankovits</a>
 */
class SmbFileRandomAccessContent extends AbstractRandomAccessContent
{
    private final SmbRandomAccessFile raf;
    private final InputStream rafis;

    SmbFileRandomAccessContent(final SmbFile smbFile, final RandomAccessMode mode) throws FileSystemException
    {
        super(mode);

        try
        {
            raf = new SmbRandomAccessFile(smbFile, mode.getModeString());
            rafis = new InputStream()
            {
                @Override
                public int read() throws IOException
                {
                    return raf.readByte();
                }

                @Override
                public long skip(long n) throws IOException
                {
                    raf.seek(raf.getFilePointer() + n);
                    return n;
                }

                @Override
                public void close() throws IOException
                {
                    raf.close();
                }

                @Override
                public int read(byte b[]) throws IOException
                {
                    return raf.read(b);
                }

                @Override
                public int read(byte b[], int off, int len) throws IOException
                {
                    return raf.read(b, off, len);
                }

                @Override
                public int available() throws IOException
                {
                    long available = raf.length() - raf.getFilePointer();
                    if (available > Integer.MAX_VALUE)
                    {
                        return Integer.MAX_VALUE;
                    }

                    return (int) available;
                }
            };
        }
        catch (MalformedURLException e)
        {
            throw new FileSystemException("vfs.provider/random-access-open-failed.error", smbFile, e);
        }
        catch (SmbException e)
        {
            throw new FileSystemException("vfs.provider/random-access-open-failed.error", smbFile, e);
        }
        catch (UnknownHostException e)
        {
            throw new FileSystemException("vfs.provider/random-access-open-failed.error", smbFile, e);
        }
    }

    @Override
	public long getFilePointer() throws IOException
    {
        return raf.getFilePointer();
    }

    @Override
	public void seek(long pos) throws IOException
    {
        raf.seek(pos);
    }

    @Override
	public long length() throws IOException
    {
        return raf.length();
    }

    @Override
	public void close() throws IOException
    {
        raf.close();
    }

    @Override
	public byte readByte() throws IOException
    {
        return raf.readByte();
    }

    @Override
	public char readChar() throws IOException
    {
        return raf.readChar();
    }

    @Override
	public double readDouble() throws IOException
    {
        return raf.readDouble();
    }

    @Override
	public float readFloat() throws IOException
    {
        return raf.readFloat();
    }

    @Override
	public int readInt() throws IOException
    {
        return raf.readInt();
    }

    @Override
	public int readUnsignedByte() throws IOException
    {
        return raf.readUnsignedByte();
    }

    @Override
	public int readUnsignedShort() throws IOException
    {
        return raf.readUnsignedShort();
    }

    @Override
	public long readLong() throws IOException
    {
        return raf.readLong();
    }

    @Override
	public short readShort() throws IOException
    {
        return raf.readShort();
    }

    @Override
	public boolean readBoolean() throws IOException
    {
        return raf.readBoolean();
    }

    @Override
	public int skipBytes(int n) throws IOException
    {
        return raf.skipBytes(n);
    }

    @Override
	public void readFully(byte b[]) throws IOException
    {
        raf.readFully(b);
    }

    @Override
	public void readFully(byte b[], int off, int len) throws IOException
    {
        raf.readFully(b, off, len);
    }

    @Override
	public String readUTF() throws IOException
    {
        return raf.readUTF();
    }

    @Override
    public void writeDouble(double v) throws IOException
    {
        raf.writeDouble(v);
    }

    @Override
    public void writeFloat(float v) throws IOException
    {
        raf.writeFloat(v);
    }

    @Override
    public void write(int b) throws IOException
    {
        raf.write(b);
    }

    @Override
    public void writeByte(int v) throws IOException
    {
        raf.writeByte(v);
    }

    @Override
    public void writeChar(int v) throws IOException
    {
        raf.writeChar(v);
    }

    @Override
    public void writeInt(int v) throws IOException
    {
        raf.writeInt(v);
    }

    @Override
    public void writeShort(int v) throws IOException
    {
        raf.writeShort(v);
    }

    @Override
    public void writeLong(long v) throws IOException
    {
        raf.writeLong(v);
    }

    @Override
    public void writeBoolean(boolean v) throws IOException
    {
        raf.writeBoolean(v);
    }

    @Override
    public void write(byte b[]) throws IOException
    {
        raf.write(b);
    }

    @Override
    public void write(byte b[], int off, int len) throws IOException
    {
        raf.write(b, off, len);
    }

    @Override
    public void writeBytes(String s) throws IOException
    {
        raf.writeBytes(s);
    }

    @Override
    public void writeChars(String s) throws IOException
    {
        raf.writeChars(s);
    }

    @Override
    public void writeUTF(String str) throws IOException
    {
        raf.writeUTF(str);
    }

    @Override
	public InputStream getInputStream() throws IOException
    {
        return rafis;
    }
}
