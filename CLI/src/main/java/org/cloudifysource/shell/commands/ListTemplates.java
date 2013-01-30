package org.cloudifysource.shell.commands;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.felix.gogo.commands.Command;
import org.cloudifysource.dsl.cloud.ComputeTemplate;
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
		Map<String, ComputeTemplate> templatesList = adminFacade.listTemplates();
		String formattedList = getTemplatesListAsString(templatesList);
	    
		return formattedList;
	}

	private String getTemplatesListAsString(
			final Map<String, ComputeTemplate> templatesList) {
		if (templatesList == null || templatesList.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder("{");
		sb.append(CloudifyConstants.NEW_LINE);
		for (Entry<String, ComputeTemplate> entry : templatesList.entrySet()) {
			String cloudTemplateStr = entry.getValue().toFormatedString();
			cloudTemplateStr = cloudTemplateStr.replaceAll(CloudifyConstants.NEW_LINE, 
					CloudifyConstants.NEW_LINE 
					+ CloudifyConstants.TAB_CHAR
					+ CloudifyConstants.TAB_CHAR);
			
			sb.append(CloudifyConstants.TAB_CHAR)
			.append(ShellUtils.getBoldMessage(entry.getKey() + ":"))
			.append(CloudifyConstants.NEW_LINE)
			.append(CloudifyConstants.TAB_CHAR)
			.append(CloudifyConstants.TAB_CHAR)
			.append(cloudTemplateStr)
			.append(",")
			.append(CloudifyConstants.NEW_LINE);
		}
		String result = sb.toString();
		result = result.substring(0, result.lastIndexOf(","));
		return result + CloudifyConstants.NEW_LINE + "}";
	}

}
