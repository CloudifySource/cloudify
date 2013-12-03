package org.cloudifysource.security;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.Assert;

public class CloudifyUserDetailsService implements UserDetailsService {

    private final Map<String, CloudifyUserDetails> users = new HashMap<String, CloudifyUserDetails>();
    
	
    public CloudifyUserDetailsService(final Collection<CloudifyUserDetails> users) {
        for (CloudifyUserDetails user : users) {
        	createUser(user);
        }
    }
    

    private void createUser(final CloudifyUserDetails cloudifyUserDetails) {
        Assert.isTrue(!userExists(cloudifyUserDetails.getUsername()));
        users.put(cloudifyUserDetails.getUsername().toLowerCase(), new CloudifyUser(cloudifyUserDetails));
    }
    

    private boolean userExists(final String username) {
        return users.containsKey(username.toLowerCase());
    }
    
 
    @Override
    public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
        CloudifyUserDetails cloudifyUserDetails = users.get(username.toLowerCase());

        if (cloudifyUserDetails == null) {
            throw new UsernameNotFoundException(username);
        }

        return new CloudifyUser(cloudifyUserDetails);
    }

}
