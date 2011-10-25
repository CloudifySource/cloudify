//---------------------------------------------------------------------------------
// Microsoft (R) Windows Azure SDK
// Software Development Kit
// 
// Copyright (c) Microsoft Corporation. All rights reserved.  
//
// THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, 
// EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES 
// OF MERCHANTABILITY AND/OR FITNESS FOR A PARTICULAR PURPOSE. 
//---------------------------------------------------------------------------------

using System;
using System.Diagnostics;
using System.IO;
using System.Configuration;
using System.Net.Security;
using Microsoft.Samples.WindowsAzure.ServiceManagement.Tools.CSManageCommands;
using System.ServiceModel;
namespace Microsoft.Samples.WindowsAzure.ServiceManagement.Tools
{
    class Program : ParseArgs
    {
        
        CSManageCommand command = new CSManageCommand();
        Program()
        {
        }

        static bool verbose = false;
        bool hasErrors = false;

        protected void LogMessage(string message, params object[] args)
        {
            Console.Out.WriteLine(message, args);
        }

        protected override void LogError(string message, params object[] args)
        {
            Console.Error.WriteLine(message, args);
            hasErrors = true;
        }

        public void ExecuteActions(string[] args)
        {
            ParseArguments(args);
            if (hasErrors)
            {
                throw new CSManageException("Use /? for command line syntax.");
            }
        }

        #region Argument Parsing Code
        private void ParseArguments(string[] args)
        {

            bool noArgs = true;
            foreach (var tok in GetTokens(args))
            {
                noArgs = false;
                if (tok.flag == null)
                {
                    continue;
                }
                
                switch (tok.flag.ToLowerInvariant())
                {
                    case "/?":
                    case "/help":
                        command.Usage();
                        return;
					case "/subscription-id":
                        {
                            CSManageCommand.SubscriptionId = tok.args[0];
                            break;
                        }
					case "/certificate-thumbprint":
                        {
                            CSManageCommand.CertificateThumbprint = tok.args[0];
                            break;
                        }                    
					case "/hosted-service":
                        {
                            CSManageCommand.HostedServiceName = tok.args[0];
                            break;
                        }
                    case "/name":
                        {
                            CSManageCommand.DeploymentName = tok.args[0];
                            break;
                        }
                    case "/label":
                        {
                            CSManageCommand.Label = tok.args[0];
                            break;
                        }
                    case "/slot":
                        {
                            CSManageCommand.DeploymentSlot = tok.args[0];
                            break;
                        }
                    case "/config":
                        {
                            CSManageCommand.ConfigFileLocation = tok.args[0];
                            break;
                        }
                    case "/op-id":
                        {
                            CSManageCommand.OperationId = tok.args[0];
                            break;
                        }
                    case "/warnings-as-error":
                        {
                            CSManageCommand.TreatWarningsAsError = true;
                            break;
                        }
                    case "/package":
                        {
                            CSManageCommand.PackageLocation = tok.args[0];
                            break;
                        }
                    case "/status":
                        {
                            CSManageCommand.DeploymentStatus = tok.args[0];
                            break;
                        }

                    case "/description":
                        {
                            CSManageCommand.Description = tok.args[0];
                            break;
                        }

                    case "/location":
                        {
                            CSManageCommand.LocationConstraintName = tok.args[0];
                            break;
                        }

                    case "/verbose":
                        {
                            verbose = true;
                            break;
                        }

                    case "/cert-file":
                       {
                            CSManageCommand.CertificateFile = tok.args[0];
                            break;
                       }

                    case "/cert-file-password":
                       {
                           CSManageCommand.CertificateFilePassword = tok.args[0];
                           break;
                       }

                    case "/storage-account":
                       {
                           CSManageCommand.StorageAccount = tok.args[0];
                           break;
                       }

                    case "/storage-key":
                       {
                           CSManageCommand.StorageKey = tok.args[0];
                           break;
                       }

                    case "/storage-container":
                       {
                           CSManageCommand.StorageContainer = tok.args[0];
                           break;
                       }
                }
                
            }
            if (noArgs)
            {
                command.Usage();
                return;
            }
            foreach (var tok in GetTokens(args))
            {
                if (tok.flag == null)
                {
                    continue;
                }
                switch (tok.flag.ToLowerInvariant())
                {
                    case "/get-deployment-config":
                        {
                            ((new GetDeploymentConfigCommand())).Run();
                            return;
                        }
                    case "/set-deployment-config":
                        {
                            (new SetDeploymentConfigCommand()).Run();
                            return;
                        }
                    case "/get-deployment-status":
                        {
                            ((new GetDeploymentStatusCommand())).Run();
                            return;
                        }
                    case "/get-deployment-url":
                        {
                            ((new GetDeploymentUrlCommand())).Run();
                            return;
                        }
                    case "/get-operation-status":
                        {
                            (new GetResultCommand()).Run();
                            return;
                        }
                    case "/create-deployment":
                        {
                            (new CreateDeploymentCommand()).Run();
                            return;
                        }
                    case "/update-deployment":
                        {
                            (new UpdateDeploymentStatusCommand()).Run();
                            return;
                        }
                    case "/delete-deployment":
                        {
                            (new DeleteDeploymentCommand()).Run();
                            return;
                        }

                    case "/create-hosted-service":
                        {
                            (new CreateHostedServiceCommand()).Run();
                            return;
                        }

                    case "/list-hosted-services":
                        {
                            (new ListHostedServicesCommand()).Run();
                            return;
                        }

                    case "/list-locations":
                        {
                            (new ListLocationsCommand()).Run();
                            return;
                        }

                    case "/list-certificates":
                        {
                            (new ListCertificateThumbprintsCommand()).Run();
                            return;
                        }

                    case "/add-certificate":
                        {
                            (new AddCertificatesCommand()).Run();
                            return;
                        }

                    default:
                        {
                            LogError("Unknown Flag {0}", tok.flag);
                            return;
                        }
                }
            }
            LogError("No command specified");
            return;
        }
        #endregion

        static int Main(string[] args)
        {
            Program prgm = new Program();
            try
            {
                ProcessCheckServerCertificate();
                prgm.ExecuteActions(args);
                return 0;
            }
            catch (CSManageException ex)
            {
                prgm.LogError("Error: {0}", verbose ? ex.ToString() : ex.Message);
            }
            catch (Exception ex)
            {
                prgm.LogError("Encountered and unexpected error {0}", verbose ? ex.ToString() : ex.Message);
            }
            return 1;
        }

        static void ProcessCheckServerCertificate()
        {
            var CheckServerCertificateString = Utility.TryGetConfigurationSetting("CheckServerCertificate");

            bool checkServerCertificate = true;

            if (!string.IsNullOrEmpty(CheckServerCertificateString))
            {
                if (!Boolean.TryParse(CheckServerCertificateString, out checkServerCertificate))
                {
                    Console.WriteLine("The value of CheckServerCertificate cannot be recognized. Using true as the default value.");
                }
            }

            System.Net.ServicePointManager.ServerCertificateValidationCallback =
               ((sender, certificate, chain, sslPolicyErrors) =>
                {
                    if (!checkServerCertificate)
                        return true;
                    if (sslPolicyErrors == SslPolicyErrors.None)
                        return true;

                    throw new CSManageException(String.Format("Certificate error: {0}", sslPolicyErrors));
                }
               );

        }
    }
}
