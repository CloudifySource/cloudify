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
    using System.Net;
    using System.Net.Security;
    using System.Security.Cryptography.X509Certificates;
    using System.ServiceModel;
    using System.ServiceModel.Web;
    using Microsoft.Samples.WindowsAzure.ServiceManagement;
    using System.IO;
    using System.Configuration;
    using Microsoft.Samples.WindowsAzure.ServiceManagement.Tools.CSManageCommands;
    using System.ServiceModel.Security;
    using System.Security.Cryptography;

    public partial class CSManageCommand
    {
        public const int PollTimeoutInSeconds = 1800;

        public CSManageCommand()
        {
        }

        //Common parameters for different commands
        public static string SubscriptionId { get; set; }
        public static void ValidateSubscriptionId()
        {
            if (string.IsNullOrEmpty(SubscriptionId))
            {
                throw new CSManageException("SubscriptionId is null or empty.");
            }
        }


        public static string Label { get; set; }
        public static void ValidateLabel(bool displayError)
        {
            if (string.IsNullOrEmpty(Label))
            {
                throw new CSManageException("Label is null or empty.");
            }
        }


        public static string CertificateThumbprint { get; set; }
        public static X509Certificate2 Certificate { get; set; }
        public static void ValidateCertificate()
        {
            if (String.IsNullOrEmpty(CertificateThumbprint))
            {
                throw new CSManageException("CertificateThumbprint is null or empty.");
            }

            Boolean certificateFound = false;
            Boolean privateKeyAvailable = false;
            CryptographicException privateKeyCheckException = null;
            X509Certificate2[] certs = null;

            StoreLocation[] locations = { StoreLocation.CurrentUser, StoreLocation.LocalMachine };

            foreach (StoreLocation location in locations) 
            {
                certs = LoadCertificatesFromStore(location);

                if (certs.Length == 1)
                {
                    certificateFound = true;

                    // check if private key is available
                    try
                    {
                        privateKeyAvailable = false;
                        var key = certs[0].PrivateKey;
                        privateKeyAvailable = true;
                    }
                    catch (CryptographicException e)
                    {
                        privateKeyCheckException = e;
                    }
                }

                if (certificateFound && privateKeyAvailable)
                {
                    break;
                }
            }

            if (!certificateFound)
            {
                throw new CSManageException("Client certificate " + CertificateThumbprint + " cannot be found in store " + StoreName.My + 
                    " of the localmachine nor in the currentuser. Please check the certificate-thumbprint argument against the installed certificates. " + 
                    "Use mmc.exe with the certificate plugin to manage installed certificates.");
            }
            else if (!privateKeyAvailable)
            {
                throw new CSManageException("Private key for client certificate " + CertificateThumbprint + " in store " + StoreName.My + 
                    " of the localmachine or in the currentuser could not be accessed. Please check the certificate-thumbprint argument against " + 
                    "the installed certificates. Use mmc.exe with the certificate plugin to manage installed certificates.", privateKeyCheckException);
            }

            Certificate = certs[0];
        }

        private static X509Certificate2[] LoadCertificatesFromStore(StoreLocation location)
        {
            X509Store certificateStore = new X509Store(StoreName.My, location);
            try
            {
                certificateStore.Open(OpenFlags.ReadOnly);
                X509Certificate2Collection certs =  certificateStore.Certificates.Find(X509FindType.FindByThumbprint, CertificateThumbprint, false);
                X509Certificate2[] result = new X509Certificate2[certs.Count];
                certs.CopyTo(result, 0);
                return result;
            }
            finally
            {
                certificateStore.Close();
            }
         }

        public static string Description { get; set; }
        public static void ValidateDescription()
        {
            if (string.IsNullOrEmpty(Description))
            {
                throw new CSManageException("Description is null or empty.");
            }
        }


        public virtual void Usage()
        {
            Console.WriteLine(ResourceFile.Usage);
        }

        public virtual void Validate() { throw new CSManageException("Validation Required"); }
        
        protected virtual void PerformOperation(IServiceManagement channel) {}
        
        public void Run()
        {
            ValidateCertificate();
            ValidateSubscriptionId();
            Validate();

            var serviceManagement = ServiceManagementHelper.CreateServiceManagementChannel("WindowsAzureEndPoint", CSManageCommand.Certificate);
            //Console.WriteLine("Using certificate: " + CSManageCommand.Certificate.SubjectName.Name);

            try
            {
                using (OperationContextScope scope = new OperationContextScope((IContextChannel)serviceManagement))
                {
                    try
                    {
                        this.PerformOperation(serviceManagement);
                    }
                    catch (CommunicationException ce)
                    {
                        ServiceManagementError error = null;
                        System.Net.HttpStatusCode httpStatusCode = 0;
                        string operationId;
                        ServiceManagementHelper.TryGetExceptionDetails(ce, out error, out httpStatusCode, out operationId);
                        RethrowCommunicationError(ce, error);
                    }
                    
                }
            }
            catch (TimeoutException e)
            {
                throw new CSManageException(e.Message, e);
            }          
        }

        protected void RethrowCommunicationError(CommunicationException ce, ServiceManagementError error)
        {
            String message = ce.Message;

            if (error != null) 
            {
                message = error.Message + " (" + error.Code + ")";
            }
            else if (ce is MessageSecurityException) 
            {
                message =
                    ce.Message + ". Check SubscriptionId is correct: " + SubscriptionId + " " +
                    "Check certificate with the thumbrint " + CertificateThumbprint + " " +
                    "is configured as management certificate in azure portal.";
                    
            }
            throw new CSManageException(message, ce);
        }

    }
}