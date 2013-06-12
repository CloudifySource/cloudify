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
package org.cloudifysource.shell.rest;

import org.cloudifysource.dsl.rest.response.UploadResponse;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/22/13
 * Time: 3:00 PM
 * <br></br>
 *
 * Interface for uploading files to the rest gateway.
 */
public interface Uploader {

    /**
     * Uploads a file to the rest server.
     * @param fileName The final file name. may be null, in this case original file name will be used.
     * @param file The file to upload.
     * @return File upload response containing an upload key.
     * @throws Exception .
     */
    UploadResponse upload(final String fileName, final File file) throws Exception;


}
