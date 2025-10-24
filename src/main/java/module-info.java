module application.chesstrainerfx {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.sql;
    //requires application.chesstrainerfx;

    opens application.chesstrainerfx to javafx.fxml;
    exports application.chesstrainerfx;

}