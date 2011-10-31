package com.gigaspaces.cloudify.shell.commands;

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.CompleterValues;
import org.apache.felix.gogo.commands.Option;

import com.gigaspaces.cloudify.shell.ShellUtils;

import java.util.Collection;

/**
 * @author rafi
 * @since 8.0.3
 */
@Command(scope = "cloudify", name = "start", description = "starts an application or service")
public class Start extends AdminAwareCommand {

    @Option(required = false, name = "component", description = "component type, press tab to see available options")
    private String applicationName;

    @Option(required = true, name = "name", description = "component name")
    private String name;

    @CompleterValues
    public Collection<String> getCompleterValues() {
        return ShellUtils.getComponentTypesAsLowerCaseStringCollection();
    }

    @Override
    protected Object doExecute() throws Exception {
//		switch (type){
//		case APPLICATION:
//			throw new UnsupportedOperationException("APPLICATION is not supported yet");
//		case SERVICE:
//			File service = ((Map<String, File>)session.get(Constants.RECIPES)).get(name);
//			if (service != null && service.exists()){
//				return adminFacade.startService(service);
//			}else{
//				return MessageFormat.format(messages.getString("service_not_found"), name);
//			}
//		default:
//			throw new IllegalArgumentException();
//		}
        return null;
    }

}
