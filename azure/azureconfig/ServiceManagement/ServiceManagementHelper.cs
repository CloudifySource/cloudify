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
using System.Collections.Generic;
using System.Linq;
using System.ServiceModel;
using System.ServiceModel.Channels;
using System.ServiceModel.Web;
using System.Text;
using Microsoft.Samples.WindowsAzure.ServiceManagement;
using System.ServiceModel.Description;
using System.Runtime.Remoting.Proxies;
using System.Runtime.Remoting.Messaging;
using System.Reflection;
using System.Net;
using System.ServiceModel.Dispatcher;
using System.Runtime.Remoting;
using System.Xml;
using System.Runtime.Serialization;
using System.Net.Security;
using System.Security.Cryptography.X509Certificates;

namespace Microsoft.Samples.WindowsAzure.ServiceManagement
{
    public static class ServiceManagementHelper
    {
        public static IServiceManagement CreateServiceManagementChannel(X509Certificate2 cert)
        {
            WebChannelFactory<IServiceManagement> factory = new WebChannelFactory<IServiceManagement>();
            factory.Endpoint.Behaviors.Add(new ClientOutputMessageInspector());
            factory.Credentials.ClientCertificate.Certificate = cert;

            var channel = factory.CreateChannel();
            return channel;
        }

        public static IServiceManagement CreateServiceManagementChannel(Binding binding, X509Certificate2 cert)
        {
            WebChannelFactory<IServiceManagement> factory = new WebChannelFactory<IServiceManagement>(binding);
            factory.Endpoint.Behaviors.Add(new ClientOutputMessageInspector());
            factory.Credentials.ClientCertificate.Certificate = cert;

            var channel = factory.CreateChannel();
            return channel;
        }

        public static IServiceManagement CreateServiceManagementChannel(ServiceEndpoint endpoint, X509Certificate2 cert)
        {
            WebChannelFactory<IServiceManagement> factory = new WebChannelFactory<IServiceManagement>(endpoint);
            factory.Endpoint.Behaviors.Add(new ClientOutputMessageInspector());
            factory.Credentials.ClientCertificate.Certificate = cert;

            var channel = factory.CreateChannel();
            return channel;
        }

        public static IServiceManagement CreateServiceManagementChannel(string endpointConfigurationName, X509Certificate2 cert)
        {
            WebChannelFactory<IServiceManagement> factory = new WebChannelFactory<IServiceManagement>(endpointConfigurationName);
            factory.Endpoint.Behaviors.Add(new ClientOutputMessageInspector());
            factory.Credentials.ClientCertificate.Certificate = cert;

            var channel = factory.CreateChannel();
            return channel;
        }

        public static IServiceManagement CreateServiceManagementChannel(Type channelType, X509Certificate2 cert)
        {
            WebChannelFactory<IServiceManagement> factory = new WebChannelFactory<IServiceManagement>(channelType);
            factory.Endpoint.Behaviors.Add(new ClientOutputMessageInspector());
            factory.Credentials.ClientCertificate.Certificate = cert;

            var channel = factory.CreateChannel();
            return channel;
        }

        public static IServiceManagement CreateServiceManagementChannel(Uri remoteUri, X509Certificate2 cert)
        {
            WebChannelFactory<IServiceManagement> factory = new WebChannelFactory<IServiceManagement>(remoteUri);
            factory.Endpoint.Behaviors.Add(new ClientOutputMessageInspector());
            factory.Credentials.ClientCertificate.Certificate = cert;

            var channel = factory.CreateChannel();
            return channel;
        }

        public static IServiceManagement CreateServiceManagementChannel(Binding binding, Uri remoteUri, X509Certificate2 cert)
        {
            WebChannelFactory<IServiceManagement> factory = new WebChannelFactory<IServiceManagement>(binding, remoteUri);
            factory.Endpoint.Behaviors.Add(new ClientOutputMessageInspector());
            factory.Credentials.ClientCertificate.Certificate = cert;

            var channel = factory.CreateChannel();
            return channel;
        }

        public static IServiceManagement CreateServiceManagementChannel(string endpointConfigurationName, Uri remoteUri, X509Certificate2 cert)
        {
            WebChannelFactory<IServiceManagement> factory = new WebChannelFactory<IServiceManagement>(endpointConfigurationName, remoteUri);
            factory.Endpoint.Behaviors.Add(new ClientOutputMessageInspector());
            factory.Credentials.ClientCertificate.Certificate = cert;

            var channel = factory.CreateChannel();
            return channel;
        }

        public static bool TryGetExceptionDetails(CommunicationException exception, out ServiceManagementError errorDetails)
        {
            HttpStatusCode httpStatusCode;
            string operationId;
            return TryGetExceptionDetails(exception, out errorDetails, out httpStatusCode, out operationId);
        }

        public static bool TryGetExceptionDetails(CommunicationException exception, out ServiceManagementError errorDetails, out HttpStatusCode httpStatusCode, out string operationId)
        {
            errorDetails = null;
            httpStatusCode = 0;
            operationId = null;

            if (exception == null)
            {
                return false;
            }

            if (exception.Message == "Internal Server Error")
            {
                httpStatusCode = HttpStatusCode.InternalServerError;
                return true;
            }

            WebException wex = exception.InnerException as WebException;

            if (wex == null)
            {
                return false;
            }

            HttpWebResponse response = wex.Response as HttpWebResponse;
            if (response == null)
            {
                return false;
            }
            
            httpStatusCode = response.StatusCode;
            if (httpStatusCode == HttpStatusCode.Forbidden)
            {
                return true;
            }

            if (response.Headers != null)
            {
                operationId = response.Headers[Constants.OperationTrackingIdHeader];
            }

            using(var s = response.GetResponseStream())
            {
                if (s.Length == 0)
                {
                    return false;
                }

                try
                {
                    using (XmlDictionaryReader reader = XmlDictionaryReader.CreateTextReader(s, new XmlDictionaryReaderQuotas()))
                    {
                        DataContractSerializer ser = new DataContractSerializer(typeof(ServiceManagementError));
                        errorDetails = (ServiceManagementError)ser.ReadObject(reader, true);
                    }
                }
                catch (SerializationException)
                {
                    return false;
                }
            }
            return true;
        }

        public static string EncodeToBase64String(string original)
        {
            return Convert.ToBase64String(Encoding.UTF8.GetBytes(original));
        }

        public static string DecodeFromBase64String(string original)
        {
            return Encoding.UTF8.GetString(Convert.FromBase64String(original));
        }
    }

    public class ClientOutputMessageInspector : IClientMessageInspector, IEndpointBehavior
    {
        #region IClientMessageInspector Members

        public void AfterReceiveReply(ref System.ServiceModel.Channels.Message reply, object correlationState) { }
        public object BeforeSendRequest(ref System.ServiceModel.Channels.Message request, IClientChannel channel)
        {
            var property = (HttpRequestMessageProperty)request.Properties[HttpRequestMessageProperty.Name];
            if (property.Headers[Constants.VersionHeaderName] == null)
            {
                property.Headers.Add(Constants.VersionHeaderName, Constants.VersionHeaderContent20101028);
            }
            return null;
        }

        #endregion

        #region IEndpointBehavior Members

        public void AddBindingParameters(ServiceEndpoint endpoint, BindingParameterCollection bindingParameters) { }

        public void ApplyClientBehavior(ServiceEndpoint endpoint, ClientRuntime clientRuntime)
        {
            clientRuntime.MessageInspectors.Add(this);
        }

        public void ApplyDispatchBehavior(ServiceEndpoint endpoint, EndpointDispatcher endpointDispatcher) { }

        public void Validate(ServiceEndpoint endpoint) { }

        #endregion

    }
}
