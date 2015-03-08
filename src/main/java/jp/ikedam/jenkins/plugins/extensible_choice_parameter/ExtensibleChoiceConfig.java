package jp.ikedam.jenkins.plugins.extensible_choice_parameter;

import hudson.Extension;
import hudson.model.Descriptor;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;

@Extension
public class ExtensibleChoiceConfig extends GlobalConfiguration
{
	private boolean disallowSystemGroovyScript;
	public ExtensibleChoiceConfig()
	{
		load();
	}
	
	public boolean getDisallowSystemGroovyScript()
	{
		return this.disallowSystemGroovyScript;
	}
	
	@Override
	public boolean configure(StaplerRequest req, JSONObject json) throws FormException
	{
		json = json.getJSONObject("extensibleChoiceParameter");
		disallowSystemGroovyScript = json.getBoolean("disallowSystemGroovyScript");
		req.bindJSON(this, json.getJSONObject("extensibleChoiceParameter"));
		save();
		return true;
	}
}
