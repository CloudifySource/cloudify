package com.gigaspaces.azure.view;

import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.ui.internal.cnf.ServerContentProvider;
import org.eclipse.wst.server.ui.internal.view.servers.ModuleServer;

import com.gigaspaces.azure.server.AzureServer;
import com.gigaspaces.azure.wizards.AzureServerWizardProperties;

public class AzureServerContentProvider extends ServerContentProvider {

	@Override
	public Object[] getChildren(Object element) {
		if (element instanceof ModuleServer) {
			ModuleServer ms = (ModuleServer) element;
			try {
				IModule[] children = ms.server.getChildModules(ms.module, null);
				int size = children.length;
				ModuleServer[] ms2 = new ModuleServer[size];
				for (int i = 0; i < size; i++) {
					int size2 = ms.module.length;
					IModule[] module = new IModule[size2 + 1];
					System.arraycopy(ms.module, 0, module, 0, size2);
					module[size2] = children[i];
					ms2[i] = new ModuleServer(ms.server, module);
				}
				return ms2;
			} catch (Exception e) {
				return null;
			}
		}
		
		IServer server = (IServer) element;
		Object adapter = ((IServer)element).getAdapter(AzureServer.class);
		
		
		if (adapter != null) {
		
			IModule[] modules = server.getModules(); 
			int size = modules.length;
			
			
			Object[] ms = new Object[size+1];
			for (int i = 0; i < size; i++) {
				ms[i] = new ModuleServer(server, new IModule[] { modules[i] });
			}
			ms[size] = AzureServerWizardProperties.getServer();
			return ms;
		}
		else
		{
			IModule[] modules = server.getModules(); 
			int size = modules.length;
			
			
			ModuleServer[] ms = new ModuleServer[size];
			for (int i = 0; i < size; i++) {
				ms[i] = new ModuleServer(server, new IModule[] { modules[i] });
			}
			return ms;
		}
	}
	
	public boolean hasChildren(Object element) {
		if (element instanceof ModuleServer) {
			// Check if the module server has child modules.
			ModuleServer curModuleServer = (ModuleServer)element;
			IServer curServer = curModuleServer.server;
			IModule[] curModule = curModuleServer.module;
			if (curServer != null &&  curModule != null) {
				IModule[] curChildModule = curServer.getChildModules(curModule, null);
				if (curChildModule != null && curChildModule.length > 0)
					return true;
				return false;
			}
			return false;
		}
		if( element instanceof IServer ) {
			Object adapter = ((IServer)element).getAdapter(AzureServer.class);
			
			
			if (adapter != null) {
			
				return true;
			}
			return ((IServer) element).getModules().length > 0;
		}
		return false;
	}

}
