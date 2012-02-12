package com.gigaspaces.azure.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jst.server.generic.ui.internal.SWTUtil;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IModuleType;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerAttributes;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.TaskModel;
import org.eclipse.wst.server.core.internal.Server;
import org.eclipse.wst.server.core.internal.ServerWorkingCopy;
import org.eclipse.wst.server.ui.internal.Messages;
import org.eclipse.wst.server.ui.internal.ServerUIPlugin;
import org.eclipse.wst.server.ui.internal.Trace;
import org.eclipse.wst.server.ui.internal.viewers.ServerComposite;
import org.eclipse.wst.server.ui.internal.wizard.WizardTaskUtil;
import org.eclipse.wst.server.ui.internal.wizard.page.NewManualServerComposite;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;

import pluginui.Activator;

import com.gigaspaces.azure.server.AzureProject;
import com.gigaspaces.azure.server.AzureProjectTools;
import com.gigaspaces.azure.server.AzureRole;

/**
 * Composite for configuring Windows Azure server properties.
 * 
 * @author idan
 *
 */
public class AzureServerWizardComposite extends Composite {

	protected static final byte MODE_EXISTING = WizardTaskUtil.MODE_EXISTING;
	protected static final byte MODE_DETECT = WizardTaskUtil.MODE_DETECT;
	protected static final byte MODE_MANUAL = WizardTaskUtil.MODE_MANUAL;
	protected static final Object ROLE = "role";
	
	protected String jreLocation;
	protected String webServerLocation;
	protected String webServerPort;
	private final IWizardHandle wizard;

	protected IModule module;
	private String launchMode;
	protected IServerWorkingCopy existingWC;
	private TaskModel taskModel;
	
	protected byte mode = MODE_EXISTING;



	public AzureServerWizardComposite(Composite parent, IWizardHandle wizardHandle) {
		super(parent, SWT.NONE);
		this.wizard = wizardHandle;
		
		wizardHandle.setTitle("Windows Azure Server");
		wizardHandle.setDescription("Windows Azure server configuration");
		
		createControls();
	}

	private void createControls() {
		
		final Composite parent = this;
		
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		this.setLayout(layout);
		this.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		GridData gridData = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
        gridData.horizontalSpan = 1;
        
        GridData fillData = new GridData(SWT.FILL, SWT.FILL, true, true);
        fillData.heightHint = 100;
        
        final Composite topComposite = new Composite(parent, SWT.NONE);
        topComposite.setLayout(new GridLayout(3, false));
        topComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
        
        
        // Azure Roles
        final Group azureProjectsGroup = new Group(parent, SWT.NONE);
        azureProjectsGroup.setLayout(new GridLayout(1, false));
        azureProjectsGroup.setLayoutData(gridData);
        azureProjectsGroup.setText("Azure Roles");
        
        final Tree rolesTree = new Tree(azureProjectsGroup, SWT.BORDER);
        rolesTree.setLayoutData(fillData);
        
        List<AzureProject> projects = AzureProjectTools.getProjects();
        
        for (AzureProject azureProject : projects) {
			
        	List<AzureRole> roles = azureProject.getRoles();
        	
        	if(roles == null || roles.isEmpty())
        		continue;
        	
        	final TreeItem projectItem = new TreeItem(rolesTree, SWT.NONE);
        	projectItem.setText(azureProject.getProject().getName());
        	projectItem.setImage(Activator.getImages().get(Activator.PROJECT_FOLDER_IMAGE));
        
        	
        	for (AzureRole azureRole : roles) {
				
        		TreeItem roleItem = new TreeItem(projectItem, SWT.NONE);
        		roleItem.setText(azureRole.getRole());
        		roleItem.setData(azureRole);
        		roleItem.setImage(Activator.getImages().get(Activator.ROLE_FOLDER_IMAGE));
			}
		}

        
        rolesTree.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent event) {
				Widget item = event.item;
				
				if(item instanceof TreeItem)
				{
					TreeItem treeItem = (TreeItem)item;
					
					if(treeItem.getData() != null)
					{
						System.out.println(treeItem.getText());
						AzureServerWizardProperties.setRole((AzureRole) treeItem.getData());
						
					}
				}
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent event) {
				System.out.println("default " + event.text);
				
			}
		
        	});

        createExistingComposite(parent);
        // list of webservers
        Button existingServerBut = new Button(parent, SWT.RADIO);
        existingServerBut.setText("Select an existing server");
        existingServerBut.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent e) {
                createExistingComposite(parent);
        	}
		});
        
        Button newServerBut = new Button(parent, SWT.RADIO);
        newServerBut.setText("Define a new server");
        newServerBut.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent e) {
        		createManualComposite(parent);
        	}
        });

        final Text jreLocationText = SWTUtil.createLabeledPath("JDK:", "<default>", topComposite);
        jreLocationText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent event) {
				jreLocation = jreLocationText.getText(); 
			}
        });
        
	}
	
	protected void createManualComposite(Composite comp) {
		Composite manualComp2 = new Composite(comp, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.horizontalSpacing = SWTUtil.convertHorizontalDLUsToPixels(this, 4);
		layout.verticalSpacing = SWTUtil.convertVerticalDLUsToPixels(this, 4);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.numColumns = 1;
		manualComp2.setLayout(layout);
		manualComp2.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		
		NewManualServerComposite manualComp = new NewManualServerComposite(manualComp2, new NewManualServerComposite.IWizardHandle2() {
			public void run(boolean fork, boolean cancelable, IRunnableWithProgress runnable) throws InterruptedException, InvocationTargetException {
				wizard.run(fork, cancelable, runnable);
			}
			public void update() {
				wizard.update();
			}
			public void setMessage(String newMessage, int newType) {
				wizard.setMessage(newMessage, newType);
			}
		}, null, module, null, true, new NewManualServerComposite.ServerSelectionListener() {
			public void serverSelected(IServerAttributes server) {
				updateTaskModel();
			}
			public void runtimeSelected(IRuntime runtime) {
				updateTaskModel();
			}
		});
		
		manualComp.setHost("localhost");

		GridData data = new GridData(GridData.FILL_BOTH);
		data.horizontalSpan = 3;
		data.heightHint = 360;
		manualComp.setLayoutData(data);
	}
	
	protected void createExistingComposite(Composite comp) {
		ServerComposite existingComp = new ServerComposite(comp, new ServerComposite.ServerSelectionListener() {
			public void serverSelected(IServer server) {
				wizard.setMessage(null, IMessageProvider.NONE);
				
				// check for compatibility
				if (server != null && module != null) {
					IStatus status = isSupportedModule(server, module);
					if (status != null) {
						if (status.getSeverity() == IStatus.ERROR)
							wizard.setMessage(status.getMessage(), IMessageProvider.ERROR);
						else if (status.getSeverity() == IStatus.WARNING)
							wizard.setMessage(status.getMessage(), IMessageProvider.WARNING);
						else if (status.getSeverity() == IStatus.INFO)
							wizard.setMessage(status.getMessage(), IMessageProvider.INFORMATION);
						server = null;
					}
				}
				
				if (existingWC != null) {
					if (server != null && server.equals(existingWC.getOriginal()))
						return;
					existingWC = null;
				}
				if (server != null)
					existingWC = server.createWorkingCopy();
				
				if(server != null)
				{
//					((ServerWorkingCopy)existingWC).disassociate();
//					existingWC.setName("Temp"); // sets the name from the current one so that the
//							// default name generation will work
//					ServerUtil.setServerDefaultName(existingWC);
//					try {
//						existingWC.save(false, null);
//					} catch (CoreException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//					System.out.println("selected " + existingWC.getName());
//					Server origServer = (Server)server;
//					Server copyServer = new Server(server.getId(), origServer.getFile(),origServer.getRuntime(),origServer.getServerType());
					AzureServerWizardProperties.setWebServer(server);
				}
				//updateTaskModel();
			}
		}, module, launchMode);
		existingComp.setIncludeIncompatibleVersions(true);
		GridData data = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL);
		data.horizontalSpan = 3;
		data.heightHint = 250;
		existingComp.setLayoutData(data);
	}

	/**
	 * Returns the status of whether the given module could be added to the server.
	 * 
	 * @param server a server
	 * @param module a module
	 * @return an IStatus representing the error or warning, or null if there are no problems
	 */
	protected static IStatus isSupportedModule(IServerAttributes server, IModule module) {
		if (server != null && module != null) {
			IServerType serverType = server.getServerType();
			IModuleType mt = module.getModuleType();
			if (!ServerUtil.isSupportedModule(serverType.getRuntimeType().getModuleTypes(), mt)) {
				String type = mt.getName();
				return new Status(IStatus.ERROR, ServerUIPlugin.PLUGIN_ID, NLS.bind(Messages.errorVersionLevel, new Object[] { type, mt.getVersion() }));
			}
			
			IModule[] rootModules = null;
			try {
				rootModules = server.getRootModules(module, null);
			} catch (CoreException ce) {
				return ce.getStatus();
			} catch (Exception e) {
				if (Trace.WARNING) {
					Trace.trace(Trace.STRING_WARNING, "Could not find root module", e);
				}
			}
			if (rootModules != null) {
				if (rootModules.length == 0)
					return new Status(IStatus.ERROR, ServerUIPlugin.PLUGIN_ID, Messages.errorRootModule);
				
				int size = rootModules.length;
				IStatus status = null;
				boolean found = false;
				for (int i = 0; i < size; i++) {
					try {
						if (server != null)
							status = server.canModifyModules(new IModule[] {rootModules[i]}, null, null);
						if (status != null && status.isOK())
							found = true;
					} catch (Exception e) {
						if (Trace.WARNING) {
							Trace.trace(Trace.STRING_WARNING, "Could not find root module", e);
						}
					}
				}
				if (!found && status != null)
					return status;
			}
		}
		return null;
	}
	

	public void setTaskModel(TaskModel model) {
		taskModel = model;
		taskModel.putObject(WizardTaskUtil.TASK_MODE, new Byte(mode));
		updateTaskModel();
	}

	
	protected void updateTaskModel() {
		if (taskModel != null) {
			IServerWorkingCopy server = getServer();
			if (server != null) {
				taskModel.putObject(TaskModel.TASK_SERVER, server);
				taskModel.putObject(TaskModel.TASK_RUNTIME, server.getRuntime());
			} else {
				taskModel.putObject(TaskModel.TASK_SERVER, null);
				taskModel.putObject(TaskModel.TASK_RUNTIME, null);
			}
		}
		wizard.update();
	}
	
	public IServerWorkingCopy getServer() {
//		if (mode == MODE_EXISTING)
			return existingWC; //existingComp.getSelectedServer();
//		else if (mode == MODE_DETECT)
//			return detectComp.getServer();
//		else
//			return manualComp.getServer();
	}
	protected void validate() {
		if (webServerPort != null && webServerPort.length() > 0) {
			try {
				Integer.parseInt(webServerPort);
				wizard.setMessage("", IMessageProvider.NONE);
			} catch (NumberFormatException e) {
				wizard.setMessage("Web server port should be a numeric value", IMessageProvider.ERROR);
			}
		}
	}

	public String getJreLocation() {
		return jreLocation;
	}
	
	public String getWebServerLocation() {
		return webServerLocation;
	}
	
	public String getWebServerPort() {
		return webServerPort;
	}
}
