using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Microsoft.Samples.WindowsAzure.ServiceManagement.Tools.CSManageCommands
{
    public class CSManageException : Exception
    {

        public CSManageException(string message, Exception inner) : base(message,inner)
        {
        }

        public CSManageException(string message) : base(message)
        {
        }
    }
}
