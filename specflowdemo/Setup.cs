using System;
using NUnit.Framework;
using OpenQA.Selenium;
using OpenQA.Selenium.Remote;
using RestSharp;
using RestSharp.Authenticators;
using TechTalk.SpecFlow;

namespace specflowdemo
{
    [Binding]
    public class Setup
    {
        private IWebDriver driver;
        private Settings _settings;

        public Setup()
        {
            _settings = new Settings();
        }
        [BeforeScenario]
        public void BeforeScenario()
        {
            if (string.IsNullOrEmpty(_settings.Browser))
            {
                driver = new OpenQA.Selenium.PhantomJS.PhantomJSDriver();
            }
            else
            {
             //driver = new OpenQA.Selenium.PhantomJS.PhantomJSDriver();
             var capabilities = new DesiredCapabilities();
              // construct the url to sauce labs
              Uri commandExecutorUri = new Uri("http://ondemand.saucelabs.com/wd/hub");
                capabilities.SetCapability("username", _settings.Username); // supply sauce labs username
                capabilities.SetCapability("accessKey", _settings.ApiKey);
                // supply sauce labs account key
                capabilities.SetCapability("name", TestContext.CurrentContext.Test.Name); // give the test a name
                capabilities.SetCapability("timeZone", "Queensland");
                capabilities.SetCapability("browserName", _settings.Browser); // "Chrome");
                capabilities.SetCapability("platform", _settings.Platform); // "Windows 10");
                capabilities.SetCapability("version", _settings.Version);
                capabilities.SetCapability("build",
                    GetType().Assembly.GetName().Name + "_" +
                    Environment.GetEnvironmentVariable("SAUCELABS_BUILD_NUMBER"));
                capabilities.SetCapability("name", ScenarioContext.Current.ScenarioInfo.Title);

                // start a new remote web driver session on sauce labs
                driver = new SessionAwareRemoteDriver(commandExecutorUri, capabilities);


            }

            //     driver.Manage().Timeouts().ImplicitlyWait(TimeSpan.FromSeconds(1));
            driver.Manage().Timeouts().ImplicitlyWait(TimeSpan.FromSeconds(30));
            ScenarioContext.Current["driver"] = driver;
        }


        [AfterScenario]
        public void AfterScenario()
        {

            if (driver is SessionAwareRemoteDriver)
           {
               var sessionId = ((SessionAwareRemoteDriver) driver).GetSessionId();
               var client = new RestClient("https://saucelabs.com/rest/v1/globalx/jobs");
               client.Authenticator = new HttpBasicAuthenticator(_settings.Username, _settings.ApiKey);

               var request = new RestRequest("{id}", Method.PUT);
               request.AddUrlSegment("id", sessionId);
               request.AddJsonBody(new {passed = ScenarioContext.Current.TestError == null});
               client.Execute(request);
           }
           driver.Dispose();
        }
    }
}
