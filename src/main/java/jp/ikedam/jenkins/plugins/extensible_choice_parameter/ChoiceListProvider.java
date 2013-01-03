package jp.ikedam.jenkins.plugins.extensible_choice_parameter;

import java.util.List;
import hudson.ExtensionPoint;
import hudson.DescriptorExtensionList;
import hudson.model.Hudson;
import hudson.model.Descriptor;
import hudson.model.AbstractDescribableImpl;

/**
 * The abstract base class of modules provides choices.
 * 
 * Create a new choice provider in following stes:
 * <ol>
 *    <li>Define a new class derived from ChoiceListProvider</li>
 *    <li>Override getChoiceList(), which returns the choices.</li>
 *    <li>Define the internal public static class named DescriptorImpl, derived from Descriptor&lt;ChoiceListProvider&gt;</li>
 *    <li>annotate the DescriptorImpl with Extension</li>
 * </ol>
 */
abstract public class ChoiceListProvider extends AbstractDescribableImpl<ChoiceListProvider> implements ExtensionPoint
{
    /**
     * Returns the choices.
     * 
     * @return the choices list.
     */
    abstract public List<String> getChoiceList();
    
    /**
     * Returns all the ChoiceListProvider subclass whose DescriptorImpl is annotated with Extension.
     * @return DescriptorExtensionList of ChoiceListProvider subclasses.
     */
    static public DescriptorExtensionList<ChoiceListProvider,Descriptor<ChoiceListProvider>> all()
    {
        return Hudson.getInstance().<ChoiceListProvider,Descriptor<ChoiceListProvider>>getDescriptorList(ChoiceListProvider.class);
    }
}
