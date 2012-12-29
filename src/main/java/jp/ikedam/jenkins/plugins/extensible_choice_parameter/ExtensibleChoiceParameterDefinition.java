package jp.ikedam.jenkins.plugins.extensible_choice_parameter;

import java.util.List;
import java.util.Arrays;

import org.apache.commons.lang.StringUtils;

import hudson.Extension;
import hudson.DescriptorExtensionList;
import hudson.model.Descriptor;
import hudson.model.ParameterValue;
import hudson.model.StringParameterValue;
import hudson.model.SimpleParameterDefinition;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import net.sf.json.JSONObject;

/**
 * 選択肢の提供方法をExtension Pointsで定義可能な選択ビルドパラメータ
 */
public class ExtensibleChoiceParameterDefinition extends SimpleParameterDefinition
{
    private static final long serialVersionUID = 1L;
    
    /**
     * ビューでの接続に使用される情報の定義。
     * resource 以下の、クラス名に対応するパスの以下のファイルを使用する。
     *   config.jelly...ジョブの設定時に使用する
     *   index.jelly...ビルド時のパラメータの指定時に使用する。
     * TODO: パラメータチェックを行う。
     */
    @Extension
    public static class DescriptorImpl extends ParameterDescriptor
    {
        /**
         * ジョブ設定でビルドパラメータの追加時に表示される項目名
         */
        @Override
        public String getDisplayName()
        {
            return Messages._ExtensibleChoiceParameterDefinition_DisplayName().toString();
        }
        
        /**
         * 利用可能なChoiceListProviderの一覧を返す
         */
        public DescriptorExtensionList<ChoiceListProvider,Descriptor<ChoiceListProvider>> getChoiceListProviderList()
        {
            return ChoiceListProvider.all();
        }
    }
    
    /**
     * この項目を編集可能にするか？
     */
    private boolean editable = false;
    
    public boolean isEditable()
    {
        return editable;
    }
    
    /**
     * 選択肢の提供方法を決定するモジュール
     */
    private ChoiceListProvider choiceListProvider = null;
    
    public ChoiceListProvider getChoiceListProvider()
    {
        return choiceListProvider;
    }
    
    /**
     * 選択肢のリストを返す。
     */
    public List<String> getChoiceList()
    {
        return getChoiceListProvider().getChoiceList();
    }
    
    /**
     * Jenkinsが画面入力からこのオブジェクトを作成するときに使用するコンストラクタ。
     * (設定から復元される時にはコンストラクタを使わずオブジェクトが直接復元される)
     */
    @DataBoundConstructor
    public ExtensibleChoiceParameterDefinition(String name, ChoiceListProvider choiceListProvider, boolean editable, String description)
    {
        super(name, description);
        
        this.choiceListProvider = choiceListProvider;
        this.editable = editable;
    }
    
    /**
     * ビルド時に使用するパラメータをユーザの入力から決定する。
     */
    @Override
    public ParameterValue createValue(StaplerRequest request, JSONObject jo)
    {
        StringParameterValue value = request.bindJSON(StringParameterValue.class, jo);
        value.setDescription(getDescription());
        if(!isEditable() && !getChoiceList().contains(value.value))
        {
            // 編集可能じゃないのに選択肢にない値が指定された！おかしい！
            throw new IllegalArgumentException("Illegal choice: " + value.value);
        }
        return value;
    }
    
    /**
     * ビルド時に使用するパラメータをユーザの入力から決定する。
     */
    @Override
    public ParameterValue createValue(String value)
    {
        if(!isEditable() && !getChoiceList().contains(value))
        {
            // 編集可能じゃないのに選択肢にない値が指定された！おかしい！
            throw new IllegalArgumentException("Illegal choice: " + value);
        }
        return new StringParameterValue(getName(), value, getDescription());
    }
    
    /**
     * デフォルトのパラメータを返す。
     * 最初の選択肢をデフォルトの選択肢とする。
     */
    @Override
    public ParameterValue getDefaultParameterValue()
    {
        return new StringParameterValue(
            getName(),
            (getChoiceList().size() > 0)?getChoiceList().get(0):null,
            getDescription()
        );
    }
}
