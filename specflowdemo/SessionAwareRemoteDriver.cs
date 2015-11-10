using System;
using OpenQA.Selenium;
using OpenQA.Selenium.Remote;

namespace specflowdemo
{
    public class SessionAwareRemoteDriver : RemoteWebDriver
    {

        public SessionAwareRemoteDriver(ICapabilities desiredCapabilities) : base(desiredCapabilities)
        {
        }

        public SessionAwareRemoteDriver(ICommandExecutor commandExecutor, ICapabilities desiredCapabilities) : base(commandExecutor, desiredCapabilities)
        {
        }

        public SessionAwareRemoteDriver(Uri remoteAddress, ICapabilities desiredCapabilities) : base(remoteAddress, desiredCapabilities)
        {
        }

        public SessionAwareRemoteDriver(Uri remoteAddress, ICapabilities desiredCapabilities, TimeSpan commandTimeout) : base(remoteAddress, desiredCapabilities, commandTimeout)
        {
        }

        public string GetSessionId()
        {
            return base.SessionId.ToString();
        }
    }
}
