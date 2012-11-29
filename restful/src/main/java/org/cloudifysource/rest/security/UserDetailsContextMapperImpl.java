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
package org.cloudifysource.rest.security;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;

public class UserDetailsContextMapperImpl implements UserDetailsContextMapper, Serializable {
    private static final long serialVersionUID = 3962976258168853954L;

    @Override
    public UserDetails mapUserFromContext(DirContextOperations ctx, String username, Collection<GrantedAuthority> authority) {

        List<GrantedAuthority> mappedAuthorities = new ArrayList<GrantedAuthority>();


        for (GrantedAuthority granted : authority) {

            if (granted.getAuthority().equalsIgnoreCase("MY USER GROUP")) {
                mappedAuthorities.add(new GrantedAuthority(){
                    private static final long serialVersionUID = 4356967414267942910L;

                    @Override
                    public String getAuthority() {
                        return "ROLE_USER";
                    } 

                });
            } else if(granted.getAuthority().equalsIgnoreCase("MY ADMIN GROUP")) {
                mappedAuthorities.add(new GrantedAuthority() {
                    private static final long serialVersionUID = -5167156646226168080L;

                    @Override
                    public String getAuthority() {
                        return "ROLE_ADMIN";
                    }
                });
            }
        }
        return new User(username, "", true, true, true, true, mappedAuthorities);
    }

    @Override
    public void mapUserToContext(UserDetails arg0, DirContextAdapter arg1) {
    }
}