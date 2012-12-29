Behaviour.specify("SELECT.staticCombobox", 'staticCombobox', 0, function(e) {
    var items = [];
    /*
     * デフォルトの実装からの変更点1
     * デフォルトの動作 - 入力が変更されるたびにAjaxでデータを取得する。
     * 変更後の動作     - 最初に1回だけオリジナルのSELECTからデータを取得する。
     */
    $A(e.getElementsByTagName("option")).each(function(o){
        items.push(o.value);
    });
    
    /*
     * デフォルトの実装からの変更点2
     * デフォルトの動作 - jellyで定義したINPUTオブジェクトにComboBoxをバインドする。
     * 変更後の動作     - jellyではSELECTを定義し、JavaScriptでINPUTを生成してComboBoxをバインドする。
     */
    var orig = e;
    var e = document.createElement("INPUT");
    for(var i = 0; i < orig.attributes.length; ++i){
        e.setAttribute(orig.attributes[i].name, orig.attributes[i].value);
    }
    e.setAttribute("value", $(orig).value);
    
    orig.parentNode.insertBefore(e, orig);
    orig.parentNode.removeChild(orig);
    
    /*
     * デフォルトの実装からの変更点3
     * デフォルトの動作 - 現在の入力にマッチする選択肢のみ表示する。
     * 変更後の動作     - 全ての選択肢を表示する。
     */
    var c = new ComboBox(e,function(value) {
        return items;
    }, {});
    
    /*
     * デフォルトの実装からの変更点4
     * デフォルトの動作 - ドロップダウンの表示後、選択箇所が最初の項目になる
     * 変更後の動作     - ドロップダウンの表示後、現在の入力にマッチする選択肢があればそれを選択する。
     */
    
    c.oldPopulateDropdown = c.populateDropdown;
    c.populateDropdown = function(){
        this.oldPopulateDropdown();
        this.selectedItemIndex = -1;
        for(var i = 0; i < this.availableItems.length; ++i){
            if(this.availableItems[i] == this.field.value){
                this.selectedItemIndex = i;
                break;
            }
        }
        this.updateSelection();
    }
    
    /*
     * デフォルトの実装からの変更点5
     * デフォルトの動作 - 入力があった場合にドロップダウンを表示する。
     * 変更後の動作     - 項目が選択されたらドロップダウンを表示する。
     */
    e.oldonfocus = e.onfocus;
    e.onfocus = function(e){
        var oldonsubmit = this.form.onsubmit;
        this.oldonfocus(e);
        if(this.form.oldonsubmit != oldonsubmit){
            // 一部のバージョンのJenkinsの不具合への対応。
            // Comboboxでformの元のonsubmitを待避するタイミングが早すぎて、
            // onsubmitの復元に失敗する。
            this.form.oldonsubmit = oldonsubmit;
        }
        this.comboBox.valueChanged();
    }
});
