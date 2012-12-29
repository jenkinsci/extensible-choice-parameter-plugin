package jp.ikedam.jenkins.plugins.extensible_choice_parameter;

import java.util.List;
import java.util.Arrays;
import org.apache.commons.lang.StringUtils;

import hudson.Extension;
import hudson.model.Descriptor;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * テキストエリアに書いて選択肢を定義する。
 * Jenkins組み込みの選択パラメータと一緒。
 */
public class TextareaChoiceListProvider extends ChoiceListProvider
{
    private static final long serialVersionUID = 1L;
    
    /**
     * ビルドパラメータの定義の際などに使用するビューの情報
     * resource フォルダ以下のクラス名に対応するフォルダから以下のファイルが使用される
     *    config.jelly
     *        ジョブ設定で、このProviderが選択された場合に表示する追加の設定項目
     *    global.jelly
     *        システム設定で表示する設定項目。
     */
    @Extension
    public static class DescriptorImpl extends Descriptor<ChoiceListProvider>
    {
        /**
         * ビルドパラメータの追加時に表示される項目名
         */
        @Override
        public String getDisplayName()
        {
            return Messages._TextareaChoiceListProvider_DisplayName().toString();
        }
    }
    
    /**
     * 設定された選択肢のリスト
     */
    private List<String> choiceList = null;
    
    @Override
    public List<String> getChoiceList(){
        return choiceList;
    }
    
    public String getChoiceListText(){
        return StringUtils.join(choiceList, "\n");
    }
    
    /**
     * Jenkinsが画面入力からこのオブジェクトを作成するときに使用するコンストラクタ。
     * (設定から復元される時にはコンストラクタを使わずオブジェクトが直接復元される)
     */
    @DataBoundConstructor
    public TextareaChoiceListProvider(String choiceListText) {
        this.choiceList = Arrays.asList(choiceListText.split("\\r?\\n"));
    }
}
