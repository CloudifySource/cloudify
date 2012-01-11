package org.cloudifysource.shell.proxy;
import java.io.IOException;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;

import com.sun.deploy.net.proxy.DeployProxySelector;
import com.sun.deploy.net.proxy.DynamicProxyManager;
import com.sun.deploy.services.PlatformType;
import com.sun.deploy.services.ServiceManager;

public class SystemDefaultProxySelector extends DeployProxySelector {

    public static void setup() {
        
        // Use windows (other platforms might work, though this hasen't been tested)
        ServiceManager.setService(PlatformType.STANDALONE_TIGER_WIN32);

        // Go fetch to system proxy settings
        DynamicProxyManager.reset();
        
        // When connection requests are made, use me as your proxy selector
        ProxySelector.setDefault(new SystemDefaultProxySelector());
        
    }
    
    // Without this override, failure to connect will cause a gui error window to pop
    @Override
    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        // Do nothing
    }
    
}