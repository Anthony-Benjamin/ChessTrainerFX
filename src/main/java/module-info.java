module application.chesstrainerfx {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.sql;



    opens application.chesstrainerfx to javafx.fxml;
    exports application.chesstrainerfx;
    exports application.chesstrainerfx.view;
    opens application.chesstrainerfx.view to javafx.fxml;
    exports application.chesstrainerfx.test;
    opens application.chesstrainerfx.test to javafx.fxml;
    exports application.chesstrainerfx.utils;
    opens application.chesstrainerfx.utils to javafx.fxml;
    exports application.chesstrainerfx.model;
    opens application.chesstrainerfx.model to javafx.fxml;
    exports application.chesstrainerfx.view.setup;
    opens application.chesstrainerfx.view.setup to javafx.fxml;
    exports application.chesstrainerfx.controller;
    opens application.chesstrainerfx.controller to javafx.fxml;

}