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

namespace Microsoft.Samples.WindowsAzure.ServiceManagement.Tools
{
    using System;

    using Microsoft.Samples.WindowsAzure.ServiceManagement;
    using System.Collections.Generic;
    using System.IO;
    using System.Net;
    using System.ServiceModel;
    using System.Globalization;
    using Microsoft.Samples.WindowsAzure.ServiceManagement.Tools.CSManageCommands;

    public partial class CSManageCommand
    {
        public static string DeploymentName { get; set; }
        public static string DeploymentSlot { get; set; }

        public static void ValidateDeploymentName()
        {
            if (string.IsNullOrEmpty(DeploymentName))
            {
                throw new CSManageException("DeploymentName is null or empty.");
            }
        }

        public static void ValidateDeploymentSlot()
        {
            if (string.IsNullOrEmpty(DeploymentSlot))
            {
                throw new CSManageException("DeploymentSlot is null or empty.");
            }
        }

        public static void ValidateDeploymentNameOrSlot()
        {
            if (string.IsNullOrEmpty(DeploymentSlot) && string.IsNullOrEmpty(DeploymentName))
            {
                throw new CSManageException("Both DeploymentName and DeploymentSlot are null or empty.");
            }
        }
            
        public static string ConfigFileLocation { get; set; }
        public static void ValidateConfigFileLocation()
        {
            if (string.IsNullOrEmpty(ConfigFileLocation))
            {
                throw new CSManageException("ConfigFileLocation is null or empty.");
            }
            else
            {
                if (!File.Exists(ConfigFileLocation))
                {
                    throw new CSManageException(String.Format("The file {0} cannot be found or the caller does not have sufficient permissions to read it. ", ConfigFileLocation));
                }
            }
        }

        public static string DeploymentStatus { get; set; }
        public static void ValidateDeploymentStatus()
        {
            if (string.IsNullOrEmpty(DeploymentStatus))
            {
                throw new CSManageException("DeploymentStatus is null or empty.");
            }
        }

        public static string PackageLocation { get; set; }
        public static void ValidatePackageLocation()
        {
            if (string.IsNullOrEmpty(PackageLocation))
            {
                throw new CSManageException("PackageUrl is null or empty.");
            }
            
            if (!Uri.IsWellFormedUriString(PackageLocation, UriKind.Absolute) && 
                !File.Exists(PackageLocation))
            {
                throw new CSManageException("PackageUrl format error. It cannot be casted to Uri and is not an existing file path: '" + PackageLocation +"'");
            }
           
        }
        
        public static bool TreatWarningsAsError { get; set; }
    }

    class CreateDeploymentCommand : CSManageCommand
    {
        public override void Validate()
        {
            ValidateHostedServiceName();
            ValidateConfigFileLocation();
            ValidateDeploymentName();
            ValidateLabel(true);
            ValidateDeploymentSlot();
            ValidatePackageLocation();
            if (!Uri.IsWellFormedUriString(PackageLocation, UriKind.Absolute))
            {
                ValidateStorage();
            }
        }

        protected override void PerformOperation(IServiceManagement channel)
        {
            CreateDeploymentInput input = new CreateDeploymentInput
            {
                Name = DeploymentName,
                StartDeployment = true,
                Configuration = Utility.GetSettings(ConfigFileLocation),
            };

            if (!string.IsNullOrEmpty(PackageLocation))
            {
                if (Uri.IsWellFormedUriString(PackageLocation, UriKind.Absolute))
                {
                    input.PackageUrl = new Uri(PackageLocation);
                }
                else 
                {
                    //TODO: Propagate timeout parameter
                    input.PackageUrl = UploadFile(PackageLocation, TimeSpan.FromMinutes(60));
                }
                
            }

            if (!string.IsNullOrEmpty(Label))
            {
                input.Label = ServiceManagementHelper.EncodeToBase64String(Label);
            }

            if (TreatWarningsAsError)
            {
                input.TreatWarningsAsError = TreatWarningsAsError;
            }

            Console.WriteLine("Creating Deployment... Name: {0}, Label: {1}", DeploymentName, Label);
            channel.CreateOrUpdateDeployment(SubscriptionId, HostedServiceName, DeploymentSlot, input);
        }
    }

    class UpdateDeploymentStatusCommand : CSManageCommand
    {

        public override void Validate()
        {
            ValidateHostedServiceName();
            ValidateDeploymentNameOrSlot();
            ValidateDeploymentStatus();
        }

        protected override void PerformOperation(IServiceManagement channel)
        {
            var input = new UpdateDeploymentStatusInput()
            {
                Status = DeploymentStatus
            };
            Console.WriteLine("Updating DeploymentStatus");
            if (!string.IsNullOrEmpty(DeploymentName))
            {
                channel.UpdateDeploymentStatus(SubscriptionId, HostedServiceName, DeploymentName, input);
            }
            else if (!string.IsNullOrEmpty(DeploymentSlot))
            {
                channel.UpdateDeploymentStatusBySlot(SubscriptionId, HostedServiceName, DeploymentSlot, input);
            }
        }
    }

    class DeleteDeploymentCommand : CSManageCommand
    {
        public override void Validate()
        {
            ValidateHostedServiceName();
            ValidateDeploymentNameOrSlot();
        }

        protected override void PerformOperation(IServiceManagement channel)
        {
            Console.WriteLine("Deleting Deployment");
            if (!string.IsNullOrEmpty(DeploymentName))
            {
                channel.DeleteDeployment(SubscriptionId, HostedServiceName, DeploymentName);
            }
            else if (!string.IsNullOrEmpty(DeploymentSlot))
            {
                channel.DeleteDeploymentBySlot(SubscriptionId, HostedServiceName, DeploymentSlot);
            }
        }
    }

   class GetDeploymentConfigCommand : CSManageCommand
    {
        public override void Validate()
        {
            ValidateHostedServiceName();
            ValidateDeploymentNameOrSlot();
        }

        protected override void PerformOperation(IServiceManagement channel)
        {
            Deployment deployment = null;

            if (!string.IsNullOrEmpty(DeploymentName))
            {
                deployment = channel.GetDeployment(SubscriptionId, HostedServiceName, DeploymentName);
            }
            else if (!string.IsNullOrEmpty(DeploymentSlot))
            {
                deployment = channel.GetDeploymentBySlot(SubscriptionId, HostedServiceName, DeploymentSlot);

            }
            String xml = new System.Text.UTF8Encoding().GetString(Convert.FromBase64String(deployment.Configuration));
            xml = xml.Replace("xml version=\"1.0\" encoding=\"utf-16\"", "xml version=\"1.0\" encoding=\"utf-8\"");
            Console.WriteLine(xml);
        }
    }

   class GetDeploymentStatusCommand : CSManageCommand
   {
       public override void Validate()
       {
           ValidateHostedServiceName();
           ValidateDeploymentNameOrSlot();
       }

       protected override void PerformOperation(IServiceManagement channel)
       {
           Deployment deployment = null;
           try
           {
               if (!string.IsNullOrEmpty(DeploymentName))
               {
                   deployment = channel.GetDeployment(SubscriptionId, HostedServiceName, DeploymentName);
               }
               else if (!string.IsNullOrEmpty(DeploymentSlot))
               {
                   deployment = channel.GetDeploymentBySlot(SubscriptionId, HostedServiceName, DeploymentSlot);
               }

               Console.WriteLine(deployment.Status);
           }
           catch (CommunicationException ce)
           {
               ServiceManagementError error = null;
               HttpStatusCode httpStatusCode = 0;
               string operationId;
               ServiceManagementHelper.TryGetExceptionDetails(ce, out error, out httpStatusCode, out operationId);
               if (error != null && httpStatusCode == HttpStatusCode.NotFound)
               {
                   Console.WriteLine(httpStatusCode);
               }
               //cannot rethrow since stream already closed.
               else
               {
                   base.RethrowCommunicationError(ce, error);
               }
           }
       }
   }

   class GetDeploymentUrlCommand : CSManageCommand
   {
       public override void Validate()
       {
           ValidateHostedServiceName();
           ValidateDeploymentNameOrSlot();
       }

       protected override void PerformOperation(IServiceManagement channel)
       {
           Deployment deployment = null;
            if (!string.IsNullOrEmpty(DeploymentName))
            {
                deployment = channel.GetDeployment(SubscriptionId, HostedServiceName, DeploymentName);
            }
            else if (!string.IsNullOrEmpty(DeploymentSlot))
            {
                deployment = channel.GetDeploymentBySlot(SubscriptionId, HostedServiceName, DeploymentSlot);
            }

            if (deployment.Url == null)
            {
                throw new CSManageException(deployment.Name + " has not started yet and does not have a load balancer URL");
            }

            Console.WriteLine(deployment.Url);
       }
   }

    class SetDeploymentConfigCommand : CSManageCommand
    {

        public override void Validate()
        {
            ValidateHostedServiceName();
            ValidateDeploymentNameOrSlot();
            ValidateConfigFileLocation();
        }

        protected override void PerformOperation(IServiceManagement channel)
        {
            var input = new ChangeConfigurationInput();
            input.Configuration = Utility.GetSettings(ConfigFileLocation);

            if (TreatWarningsAsError)
            {
                input.TreatWarningsAsError = TreatWarningsAsError;
            }

            if (!string.IsNullOrEmpty(DeploymentName))
            {
                channel.ChangeConfiguration(SubscriptionId, HostedServiceName, DeploymentName, input);
            }
            else if (!string.IsNullOrEmpty(DeploymentSlot))
            {
                channel.ChangeConfigurationBySlot(SubscriptionId, HostedServiceName, DeploymentSlot, input);
            }
        }
    }

}
