/*
 * ******************************************************************************
 *  * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *       http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  ******************************************************************************
 */

package org.cloudifysource.dsl.context;

/**
 * Class for maintaining backwards compatibility of
 * {@link org.cloudifysource.utilitydomain.context.ServiceContextFactory} between 2.6 recipes and cloudify 2.7.
 *
 * @author Eli Polonsky.
 */
public final class ServiceContextFactory {

   private ServiceContextFactory() {

   }

   /****
    * NEVER USE THIS INSIDE THE GSC. Should only be used by external scripts.
    *
    * @return A newly created service context.
    */
   public static synchronized org.cloudifysource.domain.context.ServiceContext getServiceContext() {
      return org.cloudifysource.utilitydomain.context.ServiceContextFactory.getServiceContext();
   }

}
