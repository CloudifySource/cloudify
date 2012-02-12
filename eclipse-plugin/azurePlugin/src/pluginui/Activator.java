package pluginui;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	private static final String ICONS_FOLDER = "icons/";
	public static final String PROJECT_FOLDER_IMAGE = "ProjectFolder.png";
	public static final String ROLE_FOLDER_IMAGE = "RoleFolder.gif";

	// The plug-in ID
	public static final String PLUGIN_ID = "pluginUI"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;
	
	private static final Map<String,Image> images = new ConcurrentHashMap<String,Image>();

	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
	 * )
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		
		images.put(PROJECT_FOLDER_IMAGE, loadImage(ICONS_FOLDER + PROJECT_FOLDER_IMAGE));
		images.put(ROLE_FOLDER_IMAGE, loadImage(ICONS_FOLDER + ROLE_FOLDER_IMAGE));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
	 * )
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	public static Map<String, Image> getImages() {
		return images;
	}

	public static Image loadImage(String location) throws IOException {
		URL url = Activator.getDefault().getBundle().getEntry(location);
		URL fileURL = FileLocator.toFileURL(url);
		URL resolve = FileLocator.resolve(fileURL);
		ImageDescriptor descriptor = ImageDescriptor.createFromURL(resolve);

		return descriptor.createImage();
	}

}
