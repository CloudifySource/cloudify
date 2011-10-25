using Microsoft.WindowsAzure.ServiceRuntime;
using System.IO;
using System;
using System.Linq;
using System.Diagnostics;

namespace GigaSpaces
{
    public class PortUtils
    {
        //see: http://www.gigaspaces.com/wiki/display/XAP8/How+to+Set+GigaSpaces+Over+a+Firewall
        public static readonly int XAP_LRMI_MINPORT = 6000;
        public static readonly int XAP_LRMI_MAXPORT = 7999;
        public static readonly int XAP_LUS_PORT = 4166;
        public static readonly int XAP_WEBUI_PORT = 8099;
        public static readonly int XAP_WEBSTER_PORT =9099;
        public static readonly int XAP_REGISTRY_PORT = 9298; //default is 10098
        public static readonly int XAP_RESTADMIN_PORT = 8100;

        public static String[] GetInternalEndpoints(int port)
        {
            return (from role in RoleEnvironment.Roles.Values
                    from instance in role.Instances
                    from endpoint in instance.InstanceEndpoints.Values
                    where endpoint.IPEndpoint.Port == port
                    select endpoint.IPEndpoint.Address.ToString()
                    ).ToArray();
        }

        public static String GetLocalInternalEndpoint()
        {
            return (
                   from endpoint in RoleEnvironment.CurrentRoleInstance.InstanceEndpoints.Values
                   where endpoint.IPEndpoint.Port <= XAP_LRMI_MINPORT && endpoint.IPEndpoint.Port <= XAP_LRMI_MAXPORT
                   select endpoint.IPEndpoint.Address.ToString()).First();
        }

        /// <summary>
        /// Opens an incoming tcp port in the firewall
        /// </summary>
        /// <param name="port">Port number (80) or port range (8000-8080)</param>
        public static void OpenFirewallPort(String port)
        {
            int dummy;
            String[] ports = port.Split(new String[] {"-"}, 2, StringSplitOptions.RemoveEmptyEntries);
            if (ports.Length < 1 ||
                ports.Length > 2 ||
                !Int32.TryParse(ports[0], out dummy) ||
                (ports.Length == 2 && !Int32.TryParse(ports[1], out dummy)))
            {
                throw new ArgumentException("Invalid port number or range: " + port);
            }

            Firewall("firewall add rule "+
                    "name=\"GIGASPACES-" + port + "\" " +
                    "dir=in "+
                    "action=allow "+
                    "protocol=TCP "+
                    "localport=" + port);
        }

        internal static void DisableFirewall()
        {
            Firewall("set allprofiles state off");
        }


        private static void Firewall(String advfirewallCommand)
        {
            var cmd = new FileInfo(Environment.GetEnvironmentVariable("ComSpec"));
            var args ="/c netsh advfirewall " + advfirewallCommand;
            var process = new Process()
            {
                StartInfo = new ProcessStartInfo(cmd.FullName, args)
                {
                    WorkingDirectory = cmd.Directory.FullName,
                    UseShellExecute = false,
                    RedirectStandardOutput = true,
                    RedirectStandardError = true
                }
            };
            process.Start();
            try
            {
                String output = process.StandardOutput.ReadToEnd();
                String error = process.StandardError.ReadToEnd();
                if (!output.Trim().Equals("ok.", StringComparison.OrdinalIgnoreCase))
                {
                     throw new IOException("Firewall Error: " + output + " "  + error + " commandline : " + cmd + " "  + args);
                }
            }
            finally
            {
                process.Close();
            }
        }
    }
}
