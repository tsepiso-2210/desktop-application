module com.example.mokoaleli {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.mokoaleli to javafx.fxml;
    exports com.example.mokoaleli;
}