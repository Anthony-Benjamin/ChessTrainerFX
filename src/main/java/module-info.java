module application.chesstrainerfx {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.sql;



    opens application.chesstrainerfx to javafx.fxml;
    exports application.chesstrainerfx;

}