package jp.ikedam.jenkins.plugins.extensible_choice_parameter;

import java.util.List;
import java.util.Arrays;
import java.io.Serializable;
import org.apache.commons.lang.StringUtils;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.AbstractDescribableImpl;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * グローバル選択 で使用する、各ドロップダウンのエントリ(名前、選択の中身のセット)に対応するモデル
 * TODO: パラメータチェック
 */
public class GlobalTextareaChoiceListEntry extends AbstractDescribableImpl<GlobalTextareaChoiceListEntry> implements Serializable
{
    private static final long serialVersionUID = 1L;
    
    /**
     * クラスに対するDescriptorをJenkinsに登録するためにExtensionを指定する。
     * また、resourcesフォルダ以下の、このクラスに対応するフォルダ内で以下のビューが使用される。
     *     config.jelly
     *         Jenkinsのシステム設定でこのモジュールのパラメータの指定のために使用される。
     *         複数定義されるので、複数呼び出し(定義されているエントリの数だけ)が行われる。
     *         GlobalTextareaChoiceListProvider のglobal.jellyが呼び出し元になる。
     */
    @Extension
    public static class DescriptorImpl extends Descriptor<GlobalTextareaChoiceListEntry>
    {
        /**
         * ユーザには表示されないのでなんでもよい。
         */
        @Override
        public String getDisplayName()
        {
            return "GlobalTextareaChoiceListEntry";
        }
    }
    
    /**
     * この選択セットの名称
     */
    private String name = null;
    
    public String getName()
    {
        return name;
    }
    
    /**
     * 設定された選択肢のリスト
     */
    private List<String> choiceList = null;
    
    public List<String> getChoiceList()
    {
        return choiceList;
    }
    
    /**
     * ビューの再表示時に使用される。
     */
    public String getChoiceListText()
    {
        return StringUtils.join(choiceList, "\n");
    }
    
    /**
     * Jenkinsが画面入力からこのオブジェクトを作成するときに使用するコンストラクタ。
     * (設定から復元される時にはコンストラクタを使わずオブジェクトが直接復元される)
     */
    @DataBoundConstructor
    public GlobalTextareaChoiceListEntry(String name, String choiceListText)
    {
        this.name = name;
        this.choiceList = Arrays.asList(choiceListText.split("\\r?\\n"));
    }
}
