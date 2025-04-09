module application.chesstrainerfx {
    requires javafx.controls;
    requires javafx.fxml;


    opens application.chesstrainerfx to javafx.fxml;
    exports application.chesstrainerfx;
}