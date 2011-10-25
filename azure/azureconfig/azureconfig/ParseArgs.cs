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
using System.Collections.Generic;
using System.Collections.Specialized;

namespace Microsoft.Samples.WindowsAzure.ServiceManagement.Tools
{
    public abstract class ParseArgs
    {
        internal class FlagTokens
        {
            internal string flag;
            internal string[] args;
            internal int NumberArgs
            {
                get { return (args != null ? args.Length : 0); }
            }
        }

        internal IEnumerable<FlagTokens> GetTokens(string[] args)
        {
            StringCollection rest = new StringCollection();
            for (int i = 0; i < args.Length; i++)
            {
                var arg = args[i];
                if (arg.StartsWith("/"))
                {
                    FlagTokens tok = new FlagTokens();
                    tok.flag = arg;
                    tok.args = null;
                    int flagArgsStart = arg.IndexOf(':');
                    if (flagArgsStart > 0)
                    {
                        tok.flag = arg.Substring(0, flagArgsStart);
                        tok.args = arg.Substring(flagArgsStart+1).Split(';');
                    }
                    yield return tok;
                }
                else
                {
                    rest.Add(arg);
                }
            }
            if (rest.Count > 0)
            {
                FlagTokens tok = new FlagTokens();
                tok.flag = null;
                tok.args = new string[rest.Count];
                rest.CopyTo(tok.args, 0);
                yield return tok;
            }
            yield break;
        }
  
        protected abstract void LogError(string message, params object[] args);
    }
}
