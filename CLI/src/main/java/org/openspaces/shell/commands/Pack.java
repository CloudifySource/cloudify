/**
 *
 */
package org.openspaces.shell.commands;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;

import com.gigaspaces.cloudify.dsl.internal.packaging.Packager;
import com.gigaspaces.cloudify.dsl.internal.packaging.PackagingException;

/**
 * @author rafi
 * @since 8.0.3
 */
@Command(name = "pack", scope = "cloudify", description = "Packs a recipe folder to a recipe archive")
public class Pack extends AbstractGSCommand {

    @Argument(required = true, name = "recipe-folder", description = "The recipe folder to pack")
    File recipeFolder;

    @Override
    protected Object doExecute() throws Exception {
        File packedFile = doPack(recipeFolder);
        return MessageFormat.format(messages.getString("packed_successfully"), packedFile.getAbsolutePath());
    }

    public static File doPack(File recipeDirOrFile) throws CLIException {
		try {
			return Packager.pack(recipeDirOrFile);
		} catch (IOException e) {
			throw new CLIException(e);
		} catch (PackagingException e) {
			throw new CLIException(e);
		}
    }

}
