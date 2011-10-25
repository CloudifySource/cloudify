using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Runtime.Serialization;
using System.ServiceModel;
using System.ServiceModel.Web;

namespace Microsoft.Samples.WindowsAzure.ServiceManagement
{
    [CollectionDataContract(Name = "Certificates", ItemName = "Certificate", Namespace = Constants.ServiceManagementNS)]
    public class CertificateList : List<Certificate>
    {
        public CertificateList()
        {
        }

        public CertificateList(IEnumerable<Certificate> certificateList)
            : base(certificateList)
        {
        }
    }

    [DataContract(Namespace = Constants.ServiceManagementNS)]
    public class Certificate : IExtensibleDataObject
    {
        [DataMember(Order = 1, EmitDefaultValue = false)]
        public Uri CertificateUrl;

        [DataMember(Order = 2, EmitDefaultValue = false)]
        public string Thumbprint;

        [DataMember(Order = 3, EmitDefaultValue = false)]
        public string ThumbprintAlgorithm;

        [DataMember(Order = 4, EmitDefaultValue = false)]
        public string Data;

        public ExtensionDataObject ExtensionData { get; set; }
    }

    [DataContract(Namespace = Constants.ServiceManagementNS)]
    public class CertificateFile : IExtensibleDataObject
    {
        [DataMember(Order = 1)]
        public string Data;

        [DataMember(Order = 2)]
        public string CertificateFormat { get; set; }

        [DataMember(Order = 3)]
        public string Password { get; set; }

        public ExtensionDataObject ExtensionData { get; set; }
    }

    /// <summary>
    /// The certificate related part of the API
    /// </summary>
    public partial interface IServiceManagement
    {
        /// <summary>
        /// Lists the certificates associated with a given subscription.
        /// </summary>
        [OperationContract(AsyncPattern = true)]
        [WebGet(UriTemplate = @"{subscriptionId}/services/hostedservices/{serviceName}/certificates")]
        IAsyncResult BeginListCertificates(string subscriptionId, string serviceName, AsyncCallback callback, object state);
        CertificateList EndListCertificates(IAsyncResult asyncResult);

        /// <summary>
        /// Gets public data for the given certificate.
        /// </summary>
        [OperationContract(AsyncPattern = true)]
        [WebGet(UriTemplate = @"{subscriptionId}/services/hostedservices/{serviceName}/certificates/{thumbprintalgorithm}-{thumbprint_in_hex}")]
        IAsyncResult BeginGetCertificate(string subscriptionId, string serviceName, string thumbprintalgorithm, string thumbprint_in_hex, AsyncCallback callback, object state);
        Certificate EndGetCertificate(IAsyncResult asyncResult);

        /// <summary>
        /// Adds certificates to the given subscription. 
        /// </summary>
        [OperationContract(AsyncPattern = true)]
        [WebInvoke(Method = "POST", UriTemplate = @"{subscriptionId}/services/hostedservices/{serviceName}/certificates")]
        IAsyncResult BeginAddCertificates(string subscriptionId, string serviceName, CertificateFile input, AsyncCallback callback, object state);
        void EndAddCertificates(IAsyncResult asyncResult);


        /// <summary>
        /// Deletes the given certificate.
        /// </summary>
        [OperationContract(AsyncPattern = true)]
        [WebInvoke(Method = "DELETE", UriTemplate = @"{subscriptionId}/services/hostedservices/{serviceName}/certificates/{thumbprintalgorithm}-{thumbprint_in_hex}")]
        IAsyncResult BeginDeleteCertificate(string subscriptionId, string serviceName, string thumbprintalgorithm, string thumbprint_in_hex, AsyncCallback callback, object state);
        void EndDeleteCertificate(IAsyncResult asyncResult);
    }

    public static partial class ServiceManagementExtensionMethods
    {
        public static CertificateList ListCertificates(this IServiceManagement proxy, string subscriptionId, string serviceName)
        {
            return proxy.EndListCertificates(proxy.BeginListCertificates(subscriptionId, serviceName, null, null));
        }

        public static Certificate GetCertificate(this IServiceManagement proxy, string subscriptionId, string serviceName, string algorithm, string thumbprint)
        {
            return proxy.EndGetCertificate(proxy.BeginGetCertificate(subscriptionId, serviceName, algorithm, thumbprint, null, null));
        }

        public static void AddCertificates(this IServiceManagement proxy, string subscriptionId, string serviceName, CertificateFile input)
        {
            proxy.EndAddCertificates(proxy.BeginAddCertificates(subscriptionId, serviceName, input, null, null));
        }

        public static void DeleteCertificate(this IServiceManagement proxy, string subscriptionId, string serviceName, string algorithm, string thumbprint)
        {
            proxy.EndDeleteCertificate(proxy.BeginDeleteCertificate(subscriptionId, serviceName, algorithm, thumbprint, null, null));
        }
    }
}
