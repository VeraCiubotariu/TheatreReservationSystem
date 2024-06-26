package com.example.theatrereservationsystem.gui;

import com.example.theatrereservationsystem.domain.Administrator;
import com.example.theatrereservationsystem.gui.utils.PageLoader;
import com.example.theatrereservationsystem.service.TheatreService;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.Optional;

public class AdminLoginController {
    public Label messageLabel;
    private TheatreService service;
    private Stage stage;
    public PasswordField passwordField;
    public TextField usernameTextField;

    public void setService(TheatreService service, Stage stage){
        this.service = service;
        this.stage = stage;
    }

    public void handleGoBack() {
        PageLoader.loadMainScreen(service, stage);
    }

    public void handleLogin() {
        String username = usernameTextField.getText();
        String password = passwordField.getText();

        try{
            Optional<Administrator> admin = service.login(username, password);

            if(admin.isPresent()){
                PageLoader.loadAdminMenu(service, admin.get(), stage);
            }

            else {
                messageLabel.setText("Admin with given username doesn't exist!");
            }
        } catch (Exception ex){
            messageLabel.setText(ex.getMessage());
        }
    }
}
