package jp.ikedam.jenkins.plugins.extensible_choice_parameter;

import java.util.List;
import hudson.ExtensionPoint;
import hudson.DescriptorExtensionList;
import hudson.model.Hudson;
import hudson.model.Descriptor;
import hudson.model.AbstractDescribableImpl;

/**
 * 選択肢を提供するモジュールの基底クラス。
 * 新規に選択を提供するモジュールを定義する場合、以下の手順で行う。
 * 1. ChoiceListProvider の派生クラスを定義する
 *     getChoiceList() の定義(オーバーライド)が必要
 * 2. 派生クラス内でDescriptorを定義する
 * 3. Descriptorに@Extensionをつける
 */
abstract public class ChoiceListProvider extends AbstractDescribableImpl<ChoiceListProvider> implements ExtensionPoint
{
    /**
     * 選択肢の配列を返す。
     */
    abstract public List<String> getChoiceList();
    
    /**
     * 定義されているChoiceListProviderリストを返す。
     */
    static public DescriptorExtensionList<ChoiceListProvider,Descriptor<ChoiceListProvider>> all()
    {
        return Hudson.getInstance().<ChoiceListProvider,Descriptor<ChoiceListProvider>>getDescriptorList(ChoiceListProvider.class);
    }
}
