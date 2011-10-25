using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.IO;
using Microsoft.WindowsAzure.StorageClient;
using Microsoft.WindowsAzure;
using Microsoft.Samples.WindowsAzure.ServiceManagement.Tools.CSManageCommands;

namespace Microsoft.Samples.WindowsAzure.ServiceManagement.Tools
{
    public partial class CSManageCommand
    {
        private const String AzureBlobUriFormat = "http://{0}.blob.core.windows.net";
        private const int SingleBlobUploadThresholdInBytes = 8 * 1024 * 1024;
        public static string StorageAccount { get; set; }
        public static string StorageKey { get; set; }
        public static string StorageContainer { get; set; }

        protected static void ValidateStorage()
        {
            if (string.IsNullOrEmpty(StorageAccount))
            {
                throw new CSManageException("StorageAccount is null or empty.");
            }

            if (string.IsNullOrEmpty(StorageKey))
            {
                throw new CSManageException("StorageKey is null or empty.");
            }

            if (string.IsNullOrEmpty(StorageContainer))
            {
                throw new CSManageException("StorageContainer is null or empty.");
            }
        }


        protected static Uri UploadFile(String path, TimeSpan timeout)
        {
            Console.WriteLine("Uploading " + path + " to " + StorageContainer + " Blob Storage Container");
            StorageCredentialsAccountAndKey credentials = new StorageCredentialsAccountAndKey(StorageAccount, StorageKey);
            string accountUri = string.Format(AzureBlobUriFormat , StorageAccount);
            CloudBlobClient blobStorage = new CloudBlobClient(accountUri, credentials);
            blobStorage.SingleBlobUploadThresholdInBytes = SingleBlobUploadThresholdInBytes;
            CloudBlobContainer container = blobStorage.GetContainerReference(StorageContainer);
            //TODO: Support container properties
            container.CreateIfNotExist();
            CloudBlob blob = container.GetBlobReference(Path.GetFileName(path));
            BlobRequestOptions options = new BlobRequestOptions();
            options.Timeout = timeout;
            
            blob.UploadFile(path, options);
            return blob.Uri;
        }
    }
}
