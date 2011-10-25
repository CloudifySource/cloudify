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
    using Microsoft.Samples.WindowsAzure.ServiceManagement.Tools.CSManageCommands;

    public partial class CSManageCommand
    {
        public static string CertificateFilePassword { get; set; }
        protected void ValidateCertificateFilePassword()
        {
            if (string.IsNullOrEmpty(CertificateFilePassword))
            {
                throw new CSManageException("cert-password is null or empty.");
            }
        }

        public static string CertificateFile { get; set; }
        protected void ValidateCertificateFile()
        {
            if (string.IsNullOrEmpty(CertificateFile))
            {
                throw new CSManageException("certificate filename null or empty");
            }
            else if (!File.Exists(CertificateFile))
            {
                throw new CSManageException("The file " + CertificateFile + " cannot be found. ");
            }
        }

        public static readonly string CertificateFormat = "pfx";
    }

    class ListCertificateThumbprintsCommand : CSManageCommand
    {
        public override void Validate()
        {
            ValidateHostedServiceName();
        }

        protected override void PerformOperation(IServiceManagement channel)
        {
            var certificates = channel.ListCertificates(SubscriptionId, HostedServiceName);
            foreach (var certificate in certificates) {
                Console.WriteLine(certificate.Thumbprint);
            }
        }
    }

    class AddCertificatesCommand : CSManageCommand
    {
        public override void Validate()
        {
            ValidateHostedServiceName();
            ValidateCertificateFile();
            ValidateCertificateFilePassword();
        }

        protected override void PerformOperation(IServiceManagement channel)
        {
            var input = new CertificateFile()
            {
                CertificateFormat = CertificateFormat,
                Password = CertificateFilePassword,
            };

            input.Data = Convert.ToBase64String(File.ReadAllBytes(CertificateFile));
            channel.AddCertificates(SubscriptionId, HostedServiceName, input);
        }
    }
}