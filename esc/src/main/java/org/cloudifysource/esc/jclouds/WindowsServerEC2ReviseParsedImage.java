/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
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

package org.cloudifysource.esc.jclouds;

import java.util.Map;

import javax.inject.Inject;

import org.jclouds.aws.ec2.compute.strategy.AWSEC2ReviseParsedImage;
import org.jclouds.compute.domain.ImageBuilder;
import org.jclouds.compute.domain.OperatingSystem;
import org.jclouds.compute.domain.OsFamily;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;

/**************
 * A jclouds image parser, used to process windows server images.
 * 
 * @author barakme
 * 
 */
@Singleton
public class WindowsServerEC2ReviseParsedImage extends AWSEC2ReviseParsedImage {

	/************
	 * Default constructor.
	 */
	@Inject
	public WindowsServerEC2ReviseParsedImage() {
		super(ImmutableMap.<OsFamily, Map<String, String>> of());
	}

	/*******************
	 * {@inheritDoc}
	 */
	@Override
	public void reviseParsedImage(final org.jclouds.ec2.domain.Image from, final ImageBuilder builder,
			final OsFamily family, final OperatingSystem.Builder osBuilder) {
		if (family == OsFamily.UNRECOGNIZED) {
			if (from.getName() == null) {
				return;
			}

			if (from.getName().startsWith(
					"Windows_Server-2008")) {
				// I always build Ubuntu 10.10 images
				osBuilder.family(OsFamily.WINDOWS);
				osBuilder.version("2008");

				// our image version naming convention is /us-west-1/foo/1.1.0.20110224-0001
				builder.version(from.getImageLocation().substring(
						from.getImageLocation().lastIndexOf(
								'/') + 1));
				builder.name("Windows_Server-2008");

			}
		}
	}

}