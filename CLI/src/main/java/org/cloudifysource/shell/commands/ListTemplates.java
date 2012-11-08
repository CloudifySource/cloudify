package org.cloudifysource.shell.commands;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.felix.gogo.commands.Command;
import org.cloudifysource.dsl.cloud.CloudTemplate;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.shell.ShellUtils;

/**
 * 
 * List all cloud's templates.
 * 
 * Command syntax: 
 * 			list-templates 
 * 
 * @author yael
 * 
 * @since 2.3.0
 *
 */
@Command(scope = "cloudify", name = "list-templates", description = "List all cloud's templates")
public class ListTemplates extends AdminAwareCommand {

	@Override
	protected Object doExecute() throws Exception {
		Map<String, CloudTemplate> templatesList = adminFacade.listTemplates();
		return getTemplatesListAsString(templatesList);
	}

	private String getTemplatesListAsString(
			final Map<String, CloudTemplate> templatesList) {
		StringBuilder sb = new StringBuilder();
		for (Entry<String, CloudTemplate> entry : templatesList.entrySet()) {
			sb.append(ShellUtils.getBoldMessage(entry.getKey()));
			sb.append(CloudifyConstants.NEW_LINE + CloudifyConstants.TAB_CHAR);
			String cloudTemplateStr = entry.getValue().getFormatedString();
			cloudTemplateStr = cloudTemplateStr.replaceAll(CloudifyConstants.NEW_LINE, 
					CloudifyConstants.NEW_LINE + CloudifyConstants.TAB_CHAR);
			sb.append(cloudTemplateStr);
			sb.append(CloudifyConstants.NEW_LINE);
		}
		return sb.toString();
	}

}
