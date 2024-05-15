package com.example.theatrereservationsystem.gui;

import com.example.theatrereservationsystem.domain.Administrator;
import com.example.theatrereservationsystem.service.TheatreService;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class AdminMenuController {
    private Administrator admin;
    private TheatreService service;
    private Stage stage;

    public void setService(TheatreService service, Administrator admin, Stage stage){
        this.service = service;
        this.admin = admin;
        this.stage = stage;
    }

    public void handlePastShows(ActionEvent actionEvent) {
        PageLoader.loadPastShowsScreen(service, admin, stage);
    }

    public void handleFutureShows(ActionEvent actionEvent) {
        PageLoader.loadAdminFutureShowsScreen(service, admin, stage);
    }

    public void handleExit(ActionEvent actionEvent) {
        PageLoader.loadMainScreen(service, stage);
    }
}