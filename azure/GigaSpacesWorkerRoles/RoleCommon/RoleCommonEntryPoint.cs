using System;
using System.Web;
using System.Threading;
using Microsoft.WindowsAzure;
using Microsoft.WindowsAzure.Diagnostics;
using Microsoft.WindowsAzure.Diagnostics.Management;
using Microsoft.WindowsAzure.ServiceRuntime;
using System.Linq;
using System.IO;
using Ionic.Zip;

namespace GigaSpaces
{
    public abstract class RoleCommonEntryPoint : RoleEntryPoint
    {
        #region Properties
        private const char WorkingDriveLetter = 'u';

        protected virtual int EsmMegabytesMemory
        {
            get { return 0; }
        }

        protected virtual int GsmMegabytesMemory
        {
            get { return 0; }
        }

        protected virtual int LusMegabytesMemory
        {
            get { return 0; }
        }

        protected virtual int WebuiMegabytesMemory 
        {
            get { return 0; }
        }

        private string FirewallPorts
        {
            get { return GetStringConfig("GigaSpaces.Firewall.Ports"); }
        }

        private bool UploadGsaLogs
        {
            get { return GetBooleanConfig("GigaSpaces.XAP.UploadAgentLogs"); }
        }

        private int GsaMegabytesMemory
        {
            get { return GetInt32Config("GigaSpaces.XAP.GSA.MemoryInMB"); }
        }
 
        private bool UploadAllLogs 
        {
            get { return GetBooleanConfig("GigaSpaces.XAP.UploadAllLogs"); } 
        }
        
        protected virtual int RestAdminMegabytesMemory
        {
            get { return 0; }
        }

        private int NumberOfManagementRoleInstances
        {
            get { return GetInt32Config("GigaSpaces.XAP.NumberOfManagementRoleInstances"); }
        }
        
        
        private String JdkDownloadUri
        {
            get { return GetUriConfig("GigaSpaces.JDK.DownloadUrl"); }
        }

        private String XapDownloadUri
        {
            get { return GetUriConfig("GigaSpaces.XAP.DownloadUrl"); }
        }

        private TimeSpan TraceUploadPeriod
        {
            get { return TimeSpan.Parse(GetStringConfig("GigaSpaces.WindowsAzure.TraceUploadPeriod")); }
        }

        private String WebuiContextPath
        {
            get { return GetStringConfig("GigaSpaces.XAP.WebuiContextPath"); }
        }

        private String RestAdminContextPath
        {
            get { return GetStringConfig("GigaSpaces.XAP.RestAdminContextPath"); }
        }

        CloudStorageAccount StorageAccount
        {
            get 
            { 
                return CloudStorageAccount.Parse(
                    GetStringConfig("Microsoft.WindowsAzure.Plugins.Diagnostics.ConnectionString")); 
            }
        }

        private DirectoryInfo OutputDirectory
        {
            get
            {
                String workingDirectory = GetStringConfig("GigaSpaces.WindowsAzure.WorkingDirectory");
                if (!Path.IsPathRooted(workingDirectory))
                {
                    workingDirectory = Path.Combine(WorkingDriveLetter+":\\", workingDirectory);
                }

                DirectoryInfo outputDirectory = new DirectoryInfo(workingDirectory);
                GSTrace.WriteLine("OutputDirectory=" + workingDirectory);
                return outputDirectory;
            }
        }

        private DirectoryInfo TempDirectory { get; set; }
        
        #endregion

        GridServiceAgent agent;

        /// <summary>
        /// Starts the GigaSpaces Grid Service Agent
        /// </summary>
        public override void Run()
        {
            GSTrace.WriteLine("Running " + RoleEnvironment.CurrentRoleInstance.Id);
            try
            {
                //Uncomment this line to debug agent.Stop()
                //new Thread(() => { Thread.Sleep(30000); agent.Stop(TimeSpan.FromSeconds(10)); }).Start();
                agent.Run();
            }
            catch (Exception e)
            {
                GSTrace.WriteLine(e.ToString());
                GSTrace.Flush();
                // allow enough time for the exception to flush
                Thread.Sleep(TimeSpan.FromMinutes(2));
                return;
            }
        }

        /// <summary>
        /// Downloads and UnZips the URLs defined in ZIP_DOWNLOADS configuration
        /// Installs the GigaSpaces Grid Service Agent
        /// </summary>
        public override bool OnStart()
        {
            try
            {
                // enables crash dumps for this process (not child processes)
                CrashDumps.EnableCollection(true);

                if (UploadAllLogs || UploadGsaLogs )
                {
                    ConfigureDiagnosticsTransferPeriod();
                }

                GSTrace.WriteLine("Starting " + RoleEnvironment.CurrentRoleInstance.Id);

                OpenFirewallPorts();

                CreateWorkingDrive();
                CreateTempDirectory();

                DirectoryInfo xapHome = InstallGigaSpacesXap();
                DirectoryInfo javaHome = InstallJava();

                var lookupLocators = WaitForLookupServices();

                agent = new GridServiceAgent()
                {
                    GsaMegabytesMemory = GsaMegabytesMemory,
                    GsmMegabytesMemory = GsmMegabytesMemory,
                    LusMegabytesMemory = LusMegabytesMemory,
                    EsmMegabytesMemory = EsmMegabytesMemory,
                    WebuiMegabytesMemory = WebuiMegabytesMemory,
                    LookupLocators = lookupLocators,
                    RedirectAll = UploadAllLogs, 
                    RedirectGsa = UploadGsaLogs,
                    RedirectWebui = UploadAllLogs,
                    XapHomeDirectory = xapHome,
                    JdkHomeDirectory = javaHome,
                    BatchFilesEchoOn = true,
                    RestAdminMegabytesMemory = RestAdminMegabytesMemory,
                    WebuiContextPath = WebuiContextPath,
                    RestAdminContextPath = RestAdminContextPath,
                    TempDirectory = TempDirectory
                };

                return base.OnStart();
            }
            catch (Exception e)
            {
                GSTrace.WriteLine(e.ToString());
                GSTrace.Flush();
                return false;
            }
        }

        private void CreateTempDirectory()
        {
            TempDirectory = OutputDirectory.CreateSubdirectory("temp");
        }

        private static void CreateWorkingDrive()
        {
            String localDirectory = RoleEnvironment.GetLocalResource("LocalTempFolder").RootPath;
            GSTrace.WriteLine("LocalTempFolder=" + localDirectory);
            FileUtils.SetSubstDrive(WorkingDriveLetter, localDirectory);
        }

        private void OpenFirewallPorts()
        {
            if (!String.IsNullOrEmpty(FirewallPorts))
            {
                foreach (String port in FirewallPorts.Split(new char[] { ',' }))
                {
                    if (port.Equals("*"))
                    {
                        PortUtils.DisableFirewall();
                    }
                    else
                    {
                        PortUtils.OpenFirewallPort(port);
                    }
                }
            }
        }


        private DirectoryInfo InstallJava()
        {
            GSTrace.WriteLine("Downloading Java");
            DirectoryInfo javaHome =
                FileUtils.DownloadUnzipAndFindFile(
                    JdkDownloadUri,
                    OutputDirectory,
                    "javac.exe") //look for jdk, not jre
                .Directory
                .Parent;
            return javaHome;
        }

        private DirectoryInfo InstallGigaSpacesXap()
        {
            GSTrace.WriteLine("Installing GigaSpaces Cloudify");
            DirectoryInfo xapHome =
                FileUtils.DownloadUnzipAndFindFile(
                    XapDownloadUri,
                    OutputDirectory,
                    "gs-agent.bat")
                .Directory
                .Parent;
            GSTrace.WriteLine("Finished installing GigaSpaces Cloudify");
            return xapHome;
        }

        /// <summary>
        /// Stops the Grid Service Agent
        /// </summary>
        public override void OnStop()
        {
            GSTrace.WriteLine("Stopping " + RoleEnvironment.CurrentRoleInstance.Id);
            try
            {
                if (agent != null)
                {
                    agent.Stop(TimeSpan.FromSeconds(60));
                }
            }
            catch (Exception e)
            {
                GSTrace.WriteLine(e.ToString());
                GSTrace.Flush();
                return;
            }
            base.OnStop();
        }


        /// <summary>
        /// Configures how often the traces are uploaded to the table storage
        /// http://convective.wordpress.com/2010/12/01/configuration-changes-to-windows-azure-diagnostics-in-azure-sdk-v1-3/
        /// </summary>
        private void ConfigureDiagnosticsTransferPeriod()
        {
            
            RoleInstanceDiagnosticManager roleInstanceDiagnosticManager =
                    StorageAccount.CreateRoleInstanceDiagnosticManager(
                    RoleEnvironment.DeploymentId,
                    RoleEnvironment.CurrentRoleInstance.Role.Name,
                    RoleEnvironment.CurrentRoleInstance.Id);
            DiagnosticMonitorConfiguration diagnosticMonitorConfiguration =
                roleInstanceDiagnosticManager.GetCurrentConfiguration();

            diagnosticMonitorConfiguration.Logs.ScheduledTransferPeriod = TraceUploadPeriod;

            roleInstanceDiagnosticManager.SetCurrentConfiguration(diagnosticMonitorConfiguration);
        }

        private string[] WaitForLookupServices()
        {
            var lookupLocators = new String[0];
            if (NumberOfManagementRoleInstances > 1)
            {
                while (lookupLocators.Count() < NumberOfManagementRoleInstances)
                {
                    lookupLocators = PortUtils.GetInternalEndpoints(PortUtils.XAP_LUS_PORT);

                    GSTrace.WriteLine("Number Of Management Machines =" + lookupLocators.Count());
                    Thread.Sleep(10000);
                }
            }
            return lookupLocators;
        }

        #region Config
        protected String GetUriConfig(string key)
        {
            return HttpUtility.UrlDecode(GetStringConfig(key));
        }

        protected int GetInt32Config(String key)
        {
            return Convert.ToInt32(GetStringConfig(key));
        }

        protected bool GetBooleanConfig(String key)
        {
            string value = GetStringConfig(key);
        
            return value.Equals("true", StringComparison.OrdinalIgnoreCase) ||
                   value.Equals("yes", StringComparison.OrdinalIgnoreCase);
        }

        protected string GetStringConfig(String key)
        {
            return RoleEnvironment.GetConfigurationSettingValue(key);
        }
        #endregion
    }
}
