function ViewElementFactory() {
    this.CreateViewElement = function (type) {
        var oViewElement;
        if (type === "textbox") {
            oViewElement = new TextBox();
        }
        if (type === "dropdown") {
            oViewElement = new DropDown();
        }
        oViewElement.type = type;

        return oViewElement;
    }
}



var TextBox = function () {
    this.sTextBox = "";
};

var DropDown = function () {
    this.asListValues = [];
    this.sSelectedValues = "";
    this.oOnClickFunction = null;
    this.bEnableSearchFilter = true;
    this.sDropdownName = "";
};
