println "Checking custom dsl for ${binding.repo.name}..."

println "Binding: ${binding}"
def thisJob = new BaseDefaultJenkinsPipeline(manager, binding)
//def unitTest = thisJob.CreateUnitTestJob()


def CreateCompileJob(thisJob){

	def jobName = binding.projectName + (binding.branch.isDefault ? " " : " (branch - ${binding.branchSimpleName}) ") + "Compile and Package"
	def job = manager.job(jobName)
	def repo_href = binding.repo.links.clone.find{it.name == "ssh"}.href
	def stash_href = binding.repo.links.self[0].href
	def repo_name = binding.projectName
	/*
	if(!Binding.branch.isDefault){
		//max length for special name (which we populate with branch name) is 20.   Build task will handle that
		//we care about removing invalid characters
		def cleanBranchName = Binding.branchSimpleName.replaceAll(/[\s]/, '')
		job.environmentVariables{
			env('BRANCH', cleanBranchName)
		}
	}
	*/
	job.with{
		description "Compile code for the ${binding.branchName} of ${binding.projectName}. This job is generated using Job-DSL.  Avoid making changes manually as they will be replaced on next execution. You can find the source for this project at $stash_href"
		deliveryPipelineConfiguration('build', 'build')
		triggers{
			scm("0 0 1 1 0")
		}
		wrappers {
			colorizeOutput(colorMap = 'xterm')
			preBuildCleanup{
				deleteDirectories()
			}
		}
		scm {
		  git {
		      remote {
		          url("$repo_href")
		          credentials(binding.stashCredential)
		      }
		      branch(binding.branchName)
		  }
		}
		steps{

              //because of how groovy handles inserting the msbuild steps we have to use the old way of ramming
              //the powershell in else it will be out of sequence
              //create standard build folder structure
              configure MicrosoftDevelopment.PowerShell("""

				nuget restore
				""")
			//run clean
              configure MicrosoftDevelopment.MSBuild(".Net 4.5.2", "specflowdemo.sln", "/t:clean")
			//build
              configure MicrosoftDevelopment.MSBuild("v4.0.30319", "specflowdemo.sln", "")
			//artifacts
              configure MicrosoftDevelopment.PowerShell("""
			""")
          }
		publishers {
        	archiveArtifacts '**'
    	}


	}

	defaultLogRotation(job)
	job
}

def CreateAcceptanceTestJob(thisJob, browser, version, os){
	def jobName = binding.projectName + (binding.branch.isDefault ? " " : " (branch - ${binding.branchSimpleName}) ") + "Acceptance Test (${browser} ${version} ${os})"
	def job = manager.job(jobName)
	def repo_href = binding.repo.links.clone.find{it.name == "ssh"}.href
	def stash_href = binding.repo.links.self[0].href
	def repo_name = binding.projectName
	def buildJobName = thisJob.Jobs.get(0).name

    job.with{
    	description "Unit test code for the ${binding.branchName} of ${binding.projectName}. This job is generated using Job-DSL.  Avoid making changes manually as they will be replaced on next execution. You can find the source for this project at $stash_href"
		deliveryPipelineConfiguration('build', 'acceptance test')
		parameters {
	        stringParam('SAUCELABS_BUILD_NUMBER', '\$BUILD_NUMBER')
    	}
		wrappers {
			colorizeOutput(colorMap = 'xterm')
			preBuildCleanup{
				deleteDirectories()
			}
		}
		environmentVariables(
			SAUCELABS_APIKEY: '',
			SAUCELABS_USERNAME: '',
			SAUCELABS_BROWSER: browser,
			SAUCELABS_PLATFORM: os,
			SAUCELABS_VERSION: version
		)
    	steps{
			copyArtifacts(buildJobName){
				includePatterns "**"
				buildSelector{
					latestSuccessful true
				}
			}
			batchFile("""
				\"c:/Program Files (x86)/NUnit 2.6.4/bin/nunit-console-x86.exe\" ${repo_name}/bin/debug/${repo_name}.dll /out:TestResult.txt /xml:TestResult.xml
				""")
      batchFile("""
              md SpecflowReports
            """)

      batchFile("""
        \"packages/Specflow.1.9.0/tools/specflow.exe\" nunitexecutionreport ${repo_name}/${repo_name}.csproj /out:SpecflowReports/Specflow.html
      """)
		}
		publishers{
		  publishHtml{
		    report('SpecflowReports') {
		      reportName('Specflow Report')
		      allowMissing()
		      keepAll()
		      reportFiles('Specflow.html')
		    }
		  }
		}
    }
    defaultLogRotation(job)
	job
}

public void defaultLogRotation(job){
	job.logRotator(-1, 3)
}

def Trigger(first, second){
	first.with{
		publishers {
			downstreamParameterized {
				trigger(second.name, 'SUCCESS', true){
					parameters {
                    	predefinedProp('SAUCELABS_BUILD_NUMBER', '\$BUILD_NUMBER')
                	}
				}
			}
		}
	}

	first

}
def buildJob = CreateCompileJob(thisJob)
thisJob.Jobs.put(0, buildJob)

def acceptanceTestJob1 = CreateAcceptanceTestJob(thisJob, 'Chrome', '45.0', 'Windows 10')
def acceptanceTestJob2 = CreateAcceptanceTestJob(thisJob, 'Internet Explorer', '10.0', 'Windows 7')
def acceptanceTestJob3 = CreateAcceptanceTestJob(thisJob, 'Firefox', '40.0', 'Windows 8')
thisJob.Jobs.put(1, acceptanceTestJob1)
thisJob.Jobs.put(2, acceptanceTestJob2)
thisJob.Jobs.put(3, acceptanceTestJob3)
Trigger(buildJob, acceptanceTestJob1)
Trigger(acceptanceTestJob1, acceptanceTestJob2)
Trigger(acceptanceTestJob2, acceptanceTestJob3)

thisJob
