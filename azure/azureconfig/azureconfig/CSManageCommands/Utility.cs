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
using System.IO;
using System.Net;
using System.Security;
using Microsoft.Samples.WindowsAzure.ServiceManagement;
using System.Security.Cryptography.X509Certificates;
using System.Configuration;

namespace Microsoft.Samples.WindowsAzure.ServiceManagement.Tools
{
    static class Utility
    {
        internal static void LogObject(AffinityGroup affinityGroup)
        {
            if (affinityGroup == null)
                return;

            Console.WriteLine("AffinityGroup Name:{0}", affinityGroup.Name);
            if (!string.IsNullOrEmpty(affinityGroup.Label))
            {
                Console.WriteLine("AffinityGroup Label:{0}", ServiceManagementHelper.DecodeFromBase64String(affinityGroup.Label));
            }

            Console.WriteLine("AffinityGroup Description:{0}", affinityGroup.Description);
            Console.WriteLine("AffinityGroup Location:{0}", affinityGroup.Location);
            LogObject(affinityGroup.HostedServices);
            LogObject(affinityGroup.StorageServices);
        }

        internal static void LogObject(Location location)
        {
            if (location == null)
            {
                return;
            }
            Console.WriteLine("Location Name:{0}", location.Name);
        }

        internal static void LogObject(HostedService hostedService)
        {
            if (hostedService == null)
                return;

            if (!string.IsNullOrEmpty(hostedService.ServiceName))
                Console.WriteLine("HostedService Name:{0}", hostedService.ServiceName);

            Console.WriteLine("HostedService Url:{0}", hostedService.Url.ToString());
            LogObject(hostedService.HostedServiceProperties);
            LogObject(hostedService.Deployments);
        }

        internal static void LogObject(HostedServiceProperties hostedServiceProperties)
        {
            if (hostedServiceProperties == null)
                return;

            Console.WriteLine("HostedService Label:{0}", ServiceManagementHelper.DecodeFromBase64String(hostedServiceProperties.Label));
            Console.WriteLine("HostedService Description:{0}", hostedServiceProperties.Description);

            if (!string.IsNullOrEmpty(hostedServiceProperties.AffinityGroup))
            {
                Console.WriteLine("HostedService AffinityGroupName:{0}", hostedServiceProperties.AffinityGroup);
            }

            if (!string.IsNullOrEmpty(hostedServiceProperties.Location))
            {
                Console.WriteLine("HostedService Location:{0}", hostedServiceProperties.Location);
            }
        }

        internal static void LogObject(Deployment deployment)
        {
            if (deployment == null)
                return;

            Console.WriteLine("Name:{0}", deployment.Name);
            Console.WriteLine("Label:{0}", ServiceManagementHelper.DecodeFromBase64String(deployment.Label));
            Console.WriteLine("Url:{0}", deployment.Url.ToString());
            Console.WriteLine("Status:{0}", deployment.Status);
            Console.WriteLine("DeploymentSlot:{0}", deployment.DeploymentSlot);
            Console.WriteLine("PrivateID:{0}", deployment.PrivateID);
            Console.WriteLine("UpgradeDomainCount:{0}", deployment.UpgradeDomainCount);

            LogObject(deployment.RoleList);
            LogObject(deployment.RoleInstanceList);
            LogObject(deployment.UpgradeStatus);
        }

        internal static void LogObject(RoleList roleList)
        {
            if (roleList == null)
            {
                return;
            }

            Console.WriteLine("RoleList contains {0} item(s).", roleList.Count);
            foreach (Role r in roleList)
            {
                Console.WriteLine("    RoleName: {0}", r.RoleName);
                Console.WriteLine("    OperatingSystemVersion : {0}", r.OsVersion);
            }
        }

        internal static void LogObject(RoleInstanceList roleInstanceList)
        {
            if (roleInstanceList == null)
                return;

            Console.WriteLine("RoleInstanceList contains {0} item(s).", roleInstanceList.Count);
            foreach (var obj in roleInstanceList)
            {
                Console.WriteLine("    RoleName: {0}", obj.RoleName);
                Console.WriteLine("    InstanceName: {0}", obj.InstanceName);
                Console.WriteLine("    InstanceStatus: {0}", obj.InstanceStatus);
            }
        }

        internal static void LogObject(UpgradeStatus upgradeStatus)
        {
            if (upgradeStatus == null)
                return;

            Console.WriteLine("UpgradeType: {0}", upgradeStatus.UpgradeType);
            Console.WriteLine("CurrentUpgradeDomain: {0}", upgradeStatus.CurrentUpgradeDomain);
            Console.WriteLine("CurrentUpgradeDomainState: {0}", upgradeStatus.CurrentUpgradeDomainState);
        }

        internal static void LogObject(StorageService storageService)
        {
            if (storageService == null)
                return;
            if (!string.IsNullOrEmpty(storageService.ServiceName))
                Console.WriteLine("StorageService Name:{0}", storageService.ServiceName);
            Console.WriteLine("StorageService Url:{0}", storageService.Url.ToString());

            if (storageService.StorageServiceKeys != null)
            {
                Console.WriteLine("Primary key:{0}", storageService.StorageServiceKeys.Primary);
                Console.WriteLine("Secondary key:{0}", storageService.StorageServiceKeys.Secondary);
            }
        }

        internal static void LogObject(StorageServiceList storageServiceList)
        {
            if (storageServiceList == null)
                return;

            Console.WriteLine("StorageServiceList contains {0} item(s).", storageServiceList.Count);
            foreach (var item in storageServiceList)
            {
                LogObject(item);
            }
        }

        internal static void LogObject(AffinityGroupList affinityGroupList)
        {
            if (affinityGroupList == null)
                return;

            Console.WriteLine("AffinityGroupList contains {0} item(s).", affinityGroupList.Count);
            foreach (var item in affinityGroupList)
            {
                LogObject(item);
            }
        }

        internal static void LogObject(HostedServiceList hostedServiceList)
        {
            if (hostedServiceList == null)
                return;

            Console.WriteLine("HostedServiceList contains {0} item(s).", hostedServiceList.Count);
            foreach (var item in hostedServiceList)
            {
                LogObject(item);
            }
        }

        internal static void LogObject(LocationList locationList)
        {
            if (locationList == null)
            {
                return;
            }

            Console.WriteLine("LocationList contains {0} item(s).", locationList.Count);
            foreach (var item in locationList)
            {
                LogObject(item);
            }
        }

        internal static void LogObject(DeploymentList deploymentList)
        {
            if (deploymentList == null)
                return;

            Console.WriteLine("DeploymentList contains {0} item(s).", deploymentList.Count);
            foreach (var item in deploymentList)
            {
                LogObject(item);
            }
        }

        internal static void LogObject(CertificateList certificateList)
        {
            if (certificateList == null)
                return;

            Console.WriteLine("CertificateList contains {0} item(s).", certificateList.Count);
            foreach (var item in certificateList)
            {
                LogObject(item);
            }
        }

        internal static void LogObject(Certificate certificate)
        {
            if (certificate == null)
                return;

            if (certificate.CertificateUrl != null)
            {
                Console.WriteLine("Certificate Url:{0}", certificate.CertificateUrl.ToString());
            }

            if (certificate.ThumbprintAlgorithm != null)
            {
                Console.WriteLine("Certificate ThumbprintAlgorithm:{0}", certificate.ThumbprintAlgorithm);
            }

            if (certificate.Thumbprint != null)
            {
                Console.WriteLine("Certificate Thumbprint:{0}", certificate.Thumbprint);
            }

            if (certificate.Data != null)
            {
                X509Certificate2 cert = new X509Certificate2(Convert.FromBase64String(certificate.Data));
                if (cert != null)
                {
                    Console.WriteLine("Certificate FriendlyName:{0}", cert.FriendlyName);
                    Console.WriteLine("Certificate Subject:{0}", cert.Subject);
                    Console.WriteLine("Certificate Issuer:{0}", cert.Issuer);
                    Console.WriteLine("Certificate SerialNumber:{0}", cert.SerialNumber);
                }
                Console.WriteLine("Certificate Data:{0}", certificate.Data);
            }
        }

        internal static void LogObject(OperatingSystemFamilyList operatingSystemFamilyList)
        {
            if (operatingSystemFamilyList == null)
            {
                return;
            }

            Console.WriteLine("OperatingSystemFamilyList contains {0} item(s).", operatingSystemFamilyList.Count);
            foreach (var item in operatingSystemFamilyList)
            {
                Console.WriteLine("OperatingSystemFamily Name:{0}", item.Name);
                Console.WriteLine("OperatingSystemFamily Label:{0}", ServiceManagementHelper.DecodeFromBase64String(item.Label));
                Console.WriteLine("Operating Systems in this family:");
                LogObject(item.OperatingSystems);
            }
        }

        internal static void LogObject(OperatingSystemList operatingSystemList)
        {
            if (operatingSystemList == null)
            {
                return;
            }

            Console.WriteLine("OperatingSystemList contains {0} item(s).", operatingSystemList.Count);
            foreach (var item in operatingSystemList)
            {
                LogObject(item);
            }
        }

        internal static void LogObject(OperatingSystem operatingSystem)
        {
            if (operatingSystem == null)
            {
                return;
            }

            Console.WriteLine("Operating System Version:{0}", operatingSystem.Version);
            Console.WriteLine("Operating System Label:{0}", ServiceManagementHelper.DecodeFromBase64String(operatingSystem.Label));
            Console.WriteLine("Operating System IsDefault:{0}", operatingSystem.IsDefault);
            Console.WriteLine("Operating System IsActive:{0}", operatingSystem.IsActive);
            Console.WriteLine("Operating System Family:{0}", operatingSystem.Family);
            Console.WriteLine("Operating System FamilyLabel:{0}", operatingSystem.FamilyLabel);

        }

        internal static string GetSettings(string settingsFile)
        {
            string settings = null;
            try
            {
                settings = String.Join("", File.ReadAllLines(Path.GetFullPath(settingsFile)));
            }
            catch (Exception)
            {
                Console.WriteLine("Error reading settings from file: " + settingsFile);
                throw;
            }
            return ServiceManagementHelper.EncodeToBase64String(settings);
        }

        internal static string TryGetConfigurationSetting(string configName)
        {
            var setting = ConfigurationSettings.AppSettings[configName];
            if (setting == null)
            {
                return null;
            }
            else
            {
                return setting.ToString();
            }
        }

        internal static void LogObject(MachineImageReference imageReference)
        {
            if (imageReference == null)
            {
                return;
            }
            Console.WriteLine("Image SharedAccessSignatureUrl:{0}", imageReference.SharedAccessSignatureUrl);
        }

        internal static void LogObject(MachineImageList images)
        {
            if (images == null)
            {
                return;
            }

            Console.WriteLine("ImageList contains {0} item(s)", images.Count);

            foreach (MachineImage image in images)
            {
                LogObject(image);
            }
        }

        internal static void LogObject(MachineImage image)
        {
            if (image == null)
            {
                return;
            }
            Console.WriteLine("Image Name:{0}", image.Name);
            Console.WriteLine("Image Uuid:{0}", image.Uuid);
            Console.WriteLine("Image Timestamp:{0}", image.Timestamp);
            Console.WriteLine("Image Label:{0}", image.Label);
            Console.WriteLine("Image Description:{0}", image.Description);
            Console.WriteLine("Image ParentImageName:{0}", image.ParentImageName);
            Console.WriteLine("Image ParentImageUuid:{0}", image.ParentUuid);
            Console.WriteLine("Image ParentImageTimestamp:{0}", image.ParentTimestamp);
            Console.WriteLine("Image Status:{0}", image.Status);
            Console.WriteLine("Image AffinityGroup:{0}", image.AffinityGroup);
            Console.WriteLine("Image Location:{0}", image.Location);
            Console.WriteLine("Image CompressedSizeInBytes:{0}", image.CompressedSizeInBytes);
            Console.WriteLine("Image MountedSizeInBytes:{0}", image.MountedSizeInBytes);
        }
    }
}
