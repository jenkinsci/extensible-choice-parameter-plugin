package jp.ikedam.jenkins.plugins.extensible_choice_parameter;

import java.util.List;
import java.util.ArrayList;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.ComboBoxModel;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import net.sf.json.JSONObject;

/**
 * システム設定で選択肢をプロジェクト共通に定義できる選択パラメータ
 */
public class GlobalTextareaChoiceListProvider extends ChoiceListProvider
{
    private static final long serialVersionUID = 1L;
    
    /**
     * ビルドパラメータの定義の際などに使用するビューの情報
     * resource フォルダ以下のクラス名に対応するフォルダから以下のファイルが使用される
     *    config.jelly
     *        ジョブ設定で、このProviderが選択された場合に表示する追加の設定項目
     *    global.jelly
     *        システム設定で表示する設定項目。ただし繰り返し処理のみ行い、
     *        具体的な処理はGlobalTextareaChoiceListEntryで行う。
     */
    @Extension
    public static class DescriptorImpl extends Descriptor<ChoiceListProvider>
    {
        /**
         * 設定ファイルから復元する。
         */
        public DescriptorImpl(){
            load();
        }
        
        /**
         * 使用可能な選択肢セットのリスト
         */
        private List<GlobalTextareaChoiceListEntry> choiceListEntryList;
        
        public List<GlobalTextareaChoiceListEntry> getChoiceListEntryList()
        {
            return choiceListEntryList;
        }
        
        /**
         * 設定保存時にconfigureから呼び出す。
         */
        public void setChoiceListEntryList(List<GlobalTextareaChoiceListEntry> choiceListEntryList){
            this.choiceListEntryList = choiceListEntryList;
        }
        
        /**
         * ジョブ設定で使用する選択肢セットを表示するために、
         * 定義されている選択肢セットを一覧を作成する
         */
        public ListBoxModel doFillNameItems()
        {
            ListBoxModel m = new ListBoxModel();
            if(getChoiceListEntryList() != null){
                for(GlobalTextareaChoiceListEntry e: getChoiceListEntryList())
                {
                    m.add(e.getName());
                }
            }
            return m;
        }
        
        /**
         * 選択肢の名前から選択肢の設定を取得する
         */
        public GlobalTextareaChoiceListEntry getChoiceListEntry(String name)
        {
            for(GlobalTextareaChoiceListEntry e: getChoiceListEntryList())
            {
                if(e.getName().equals(name))
                {
                    return e;
                }
            }
            return null;
         }
        
        /**
         * 名前から選択肢のリストを取得する
         */
        public List<String> getChoiceList(String name)
        {
           GlobalTextareaChoiceListEntry e = getChoiceListEntry(name);
           return (e != null)?e.getChoiceList():new ArrayList<String>();
        }
        
        /**
         * ビルドパラメータの追加時に表示される項目名
         */
        @Override
        public String getDisplayName()
        {
            return Messages._GlobalTextareaChoiceListProvider_DisplayName().toString();
        }
        
        /**
         * システム設定で渡された設定を保存する。
         */
        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            java.util.logging.Logger logger = java.util.logging.Logger.getLogger(GlobalTextareaChoiceListProvider.class.getName());
            setChoiceListEntryList(req.bindJSONToList(GlobalTextareaChoiceListEntry.class, formData.get("choiceListEntryList")));
            
            // TODO: ここでパラメータチェックを行う
            save();
            
            return super.configure(req,formData);
        }
    }
    
    /**
     * 指定された選択肢名
     */
    private String name = null;
    
    public String getName(){
        return name;
    }
    
    @Override
    public List<String> getChoiceList(){
        return ((DescriptorImpl)getDescriptor()).getChoiceList(getName());
    }
    
    /**
     * Jenkinsが画面入力からこのオブジェクトを作成するときに使用するコンストラクタ。
     * (設定から復元される時にはコンストラクタを使わずオブジェクトが直接復元される)
     */
    @DataBoundConstructor
    public GlobalTextareaChoiceListProvider(String name) {
        this.name = name;
    }
}
