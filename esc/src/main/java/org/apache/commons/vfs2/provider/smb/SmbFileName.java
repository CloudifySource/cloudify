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

import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.provider.GenericFileName;

/**
 * An SMB URI.  Adds a share name to the generic URI.
 * CHECKSTYLE:OFF
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 */
public class SmbFileName extends GenericFileName
{
    private static final int DEFAULT_PORT = 139;

    private final String share;
    private final String domain;
    private String uriWithoutAuth;

    protected SmbFileName(
        final String scheme,
        final String hostName,
        final int port,
        final String userName,
        final String password,
        final String domain,
        final String share,
        final String path,
        final FileType type)
    {
        super(scheme, hostName, port, DEFAULT_PORT, userName, password, path, type);
        this.share = share;
        this.domain = domain;
    }

    /**
     * Returns the share name.
     */
    public String getShare()
    {
        return share;
    }

    /**
     * Builds the root URI for this file name.
     */
    @Override
    protected void appendRootUri(final StringBuilder buffer, boolean addPassword)
    {
        super.appendRootUri(buffer, addPassword);
        buffer.append('/');
        buffer.append(share);
    }

    /**
     * put domain before username if both are set
     */
    @Override
    protected void appendCredentials(StringBuilder buffer, boolean addPassword)
    {
        if (getDomain() != null && !getDomain().isEmpty() &&
            getUserName() != null && !getUserName().isEmpty())
        {
            buffer.append(getDomain());
            buffer.append("\\");
        }
        super.appendCredentials(buffer, addPassword);
    }

    /**
     * Factory method for creating name instances.
     */
    @Override
    public FileName createName(final String path, FileType type)
    {
        return new SmbFileName(
            getScheme(),
            getHostName(),
            getPort(),
            getUserName(),
            getPassword(),
            domain,
            share,
            path,
            type);
    }

    /**
     * Construct the path suitable for SmbFile when used with NtlmPasswordAuthentication
     */
    public String getUriWithoutAuth() throws FileSystemException
    {
        if (uriWithoutAuth != null)
        {
            return uriWithoutAuth;
        }

        StringBuilder sb = new StringBuilder(120);
        sb.append(getScheme());
        sb.append("://");
        sb.append(getHostName());
        if (getPort() != DEFAULT_PORT)
        {
            sb.append(":");
            sb.append(getPort());
        }
        sb.append("/");
        sb.append(getShare());
        sb.append(getPathDecoded());
        uriWithoutAuth = sb.toString();
        return uriWithoutAuth;
    }

    /**
     * returns the domain name
     */
    public String getDomain()
    {
        return domain;
    }
}
