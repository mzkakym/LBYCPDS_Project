package org.example.lbycpds_project.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.lbycpds_project.models.User;
import org.example.lbycpds_project.models.BorrowerPortal;
import org.example.lbycpds_project.utils.Data_Store;
import org.example.lbycpds_project.utils.User_File;
import java.io.IOException;

public class Login_controller {
    @FXML
    private TextField userIDField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label messageLabel;
    @FXML
    private Button backButton; // soon?


    // FORGOTTEN PASSWORD FIELDS
    @FXML
    private TextField recoveryUserIDField;
    @FXML
    private TextField recoveryContactField;
    @FXML
    private PasswordField newPasswordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private Label recoveryMessageLabel;
    @FXML
    private Button resetPasswordButton;
    @FXML
    private Button recoveryBackButton;


    // CREATE ACCOUNT FIELDS
    @FXML
    private TextField accountUserIDField;
    @FXML
    private TextField accountFullNameField;
    @FXML
    private TextField accountDepartmentField;
    @FXML
    private TextField accountContactField;
    @FXML
    private PasswordField accountPasswordField;
    @FXML
    private PasswordField accountConfirmPasswordField;
    @FXML
    private Label accountMessageLabel;


    // INITIALIZE
    @FXML
    public void initialize() {
        if (messageLabel != null) {
            messageLabel.setText("");
        }
        if (recoveryMessageLabel != null) {
            recoveryMessageLabel.setText("");
        }
        if (accountMessageLabel != null) {
            accountMessageLabel.setText("");
        }
    }


    // LOGIN FUNCTION
    @FXML
    private void handleLogin() {
        String userID = userIDField.getText().trim();
        String password = passwordField.getText();

        // CHECK EMPTY FIELDS
        if (userID.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Please enter your User ID and Password.");
            return;
        }

        // SEARCH FOR USER
        User user = User_File.findUser(userID);
        if (user == null) {
            openPage("/fxmls/login/Error_View.fxml");
            return;
        }

        // CHECK PASSWORD
        if (!user.getPassword().equals(password)) {
            openPage("/fxmls/login/Error_View.fxml");
            return;
        }

        // CHECK ACCOUNT STATUS
        if (!user.getStatus().equalsIgnoreCase("VERIFIED")) {
            messageLabel.setText("Your account has not been verified."); //check users.txt
            return;
        }

        // LOGIN SUCCESSFUL
        messageLabel.setText("Login successful!");
        System.out.println("User logged in: " + user.getFullName());
        System.out.println("User role: " + user.getRole());


        // DASHBOARD REDIRECTION (by role)
        switch (user.getRole().toUpperCase()) {
            case "BORROWER":
                openBorrowerDashboard(user);
                break;
            case "STAFF":
                messageLabel.setText("Staff dashboard is not available yet.");
                break;
            case "ADMIN":
                messageLabel.setText("Admin dashboard is not available yet.");
                break;
            default:
                messageLabel.setText("Unknown account role: " + user.getRole());
        }
    }


    // FORGOT PASSWORD FUNCTION
    @FXML
    private void handleForgotPassword() {
        openPage("/fxmls/login/Forgotten_Password.fxml");
    }


    // ACTIVATE ACCOUNT FUNCTION
    @FXML
    private void handleCreateAccount() {
        openPage("/fxmls/login/Create_Account.fxml");
    }


    // RESET PASSWORD FUNCTION
    @FXML
    private void handleResetPassword() {
        String userID = recoveryUserIDField.getText().trim();
        String contact = recoveryContactField.getText().trim();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // CHECK EMPTY FIELDS
        if (userID.isEmpty() || contact.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            recoveryMessageLabel.setText("Please complete all fields.");
            return;
        }

        // CHECK IF PASSWORDS MATCH
        if (!newPassword.equals(confirmPassword)) {
            recoveryMessageLabel.setText("Passwords do not match.");
            return;
        }

        // SEARCH FOR USER
        User user = User_File.findUser(userID);
        if (user == null) {
            recoveryMessageLabel.setText("User ID not found.");
            return;
        }

        // VERIFY CONTACT INFORMATION
        if (!user.getContact().equals(contact)) {
            recoveryMessageLabel.setText("Incorrect contact information.");
            return;
        }

        // UPDATE PASSWORD IN USERS.TXT
        boolean updated = User_File.updatePassword(userID, newPassword);
        if (updated) {
            recoveryMessageLabel.setText("Password successfully changed!");
            System.out.println("Password updated for User ID: " + userID);
        } else {
            recoveryMessageLabel.setText("Unable to update password. Please try again.");
        }
    }


    // CREATE ACCOUNT FUNCTION
    @FXML
    private void handleRegisterAccount() {
        String userID = accountUserIDField.getText().trim();
        String fullName = accountFullNameField.getText().trim();
        String department = accountDepartmentField.getText().trim();
        String contact = accountContactField.getText().trim();
        String password = accountPasswordField.getText();
        String confirmPassword = accountConfirmPasswordField.getText();

        // CHECK EMPTY FIELDS
        if (userID.isEmpty() || fullName.isEmpty() || department.isEmpty() || contact.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            accountMessageLabel.setText("Please complete all fields.");
            return;
        }

        // CHECK IF PASSWORDS MATCH
        if (!password.equals(confirmPassword)) {
            accountMessageLabel.setText("Passwords do not match.");
            return;
        }

        // CHECK IF USER ID ALREADY EXISTS
        User existingUser = User_File.findUser(userID);
        if (existingUser != null) {
            accountMessageLabel.setText("User ID already exists.");
            return;
        }

        // SAVE NEW ACCOUNT
        boolean registered = User_File.addUser(userID, password, "BORROWER", fullName, department, contact, "PENDING");
        if (registered) {
            accountMessageLabel.setText("Account created! Please wait for verification.");
            System.out.println("New account registered: " + userID);
            accountUserIDField.clear();
            accountFullNameField.clear();
            accountDepartmentField.clear();
            accountContactField.clear();
            accountPasswordField.clear();
            accountConfirmPasswordField.clear();
        } else {
            accountMessageLabel.setText("Unable to create account. Please try again.");
        }
    }


    // BACK TO LOGIN
    @FXML
    private void handleBackToLogin(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxmls/login/Login_View.fxml"));
            Parent root = loader.load();
            Node source = (Node) event.getSource();
            Stage stage = (Stage) source.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("Electronics Inventory System");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // OPEN BORROWER DASHBOARD (passes the logged-in user to the borrower screen)
    private void openBorrowerDashboard(User user) {
        Data_Store store = Data_Store.getInstance();
        Stage stage = (Stage) userIDField.getScene().getWindow();
        BorrowerPortal portal = new BorrowerPortal(
                user.getUserID(),
                user.getFullName(),
                store.getInventory(),
                store.getStaffQueue(),
                store.getReservationQueue(),
                store.getAllRequests(),
                () -> returnToLogin(stage)
        );
        stage.setScene(new Scene(portal, 1280, 720));
        stage.setTitle("Electronics Inventory System - Borrower");
        stage.centerOnScreen();
    }

    private void returnToLogin(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxmls/login/Login_View.fxml"));
            Parent root = loader.load();
            stage.getScene().setRoot(root);
            stage.setTitle("Electronics Inventory System");
        } catch (IOException e) {
            e.printStackTrace();
            messageLabel.setText("Unable to return to the login page.");
        }
    }


    // PAGE NAVIGATION
    private void openPage(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) userIDField.getScene().getWindow();
            Scene scene = new Scene(root, 1280, 720);
            stage.setScene(scene);
            stage.setTitle("Electronics Inventory System");
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            if (messageLabel != null) {
                messageLabel.setText("Unable to open the requested page.");
            }
        }
    }
}
