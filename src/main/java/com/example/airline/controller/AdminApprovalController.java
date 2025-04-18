package com.example.airline.controller;

import com.example.airline.model.entity.User;
import com.example.airline.model.service.UserService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.Button;
import javafx.scene.control.Alert; // Import Alert
import javafx.stage.Stage;
import javafx.stage.Window; // Import Window

import java.io.IOException;

public class AdminApprovalController {

    @FXML private ListView<String> pendingAdminsList;
    @FXML private Button approveButton;
    @FXML private Button manageUsersButton; // New Button
    @FXML private Button backToLoginButton; // Renamed for clarity
    @FXML private Button logoutButton;

    private final UserService userService = new UserService();
    private User loggedInUser; // Should be the root admin

    public void setLoggedInUser(User user) {
        this.loggedInUser = user;
        // Only root admin should be here
        if (loggedInUser == null || !"admin".equals(loggedInUser.getUsername())) {
            System.err.println("Unauthorized access attempt to admin approval page by: " + (user != null ? user.getUsername() : "null"));
            // Optionally disable all controls or navigate away
            approveButton.setDisable(true);
            manageUsersButton.setDisable(true);
            pendingAdminsList.setDisable(true);
            showAlert(Alert.AlertType.ERROR, "Access Denied", "Only the root admin can access this page.");
            // Maybe force logout or back to login? handleBackToAdminLogin();
        }
    }

    public void loadPendingAdmins() {
        if (loggedInUser != null && "admin".equals(loggedInUser.getUsername())) {
            pendingAdminsList.setItems(FXCollections.observableList(
                    userService.getAllPendingAdmins().stream().map(User::getUsername).toList()
            ));
        } else {
            pendingAdminsList.getItems().clear(); // Clear if not root admin
        }
    }

    @FXML
    private void handleApprove() {
        String selectedAdminUsername = pendingAdminsList.getSelectionModel().getSelectedItem();
        if (selectedAdminUsername != null) {
            if (userService.approveAdmin(selectedAdminUsername)) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Admin '" + selectedAdminUsername + "' approved.");
                loadPendingAdmins(); // Refresh the list
            } else {
                showAlert(Alert.AlertType.ERROR, "Approval Failed", "Could not approve admin: " + selectedAdminUsername);
            }
        } else {
            showAlert(Alert.AlertType.WARNING, "Selection Required", "Please select an admin from the list to approve.");
        }
    }

    // --- NEW HANDLER ---
    @FXML
    private void handleManageUsers() {
        if (loggedInUser == null || !"admin".equals(loggedInUser.getUsername())) {
            showAlert(Alert.AlertType.ERROR, "Access Denied", "Only the root admin can manage users.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/airline/view/admin_user_management.fxml"));
            Parent root = loader.load();

            AdminUserManagementController controller = loader.getController();
            if (controller != null) {
                controller.setLoggedInUser(loggedInUser); // Pass root admin user
                controller.loadAllUsers(); // Load data
            } else {
                throw new IllegalStateException("AdminUserManagementController null after loading.");
            }

            // Get current stage to switch scene
            Stage stage = (Stage) manageUsersButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("User Management");

        } catch (IOException | IllegalStateException e) {
            System.err.println("Error loading User Management screen: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Load Error", "Could not open the User Management screen.");
        }

    }
    // --- END NEW HANDLER ---

    @FXML
    private void handleBackToAdminLogin() { // Usually root admin doesn't log in again? Maybe just logout?
        // For consistency, let's go back to the main welcome screen via logout
        handleLogout();
        // Original navigation if preferred:
        // loadScene("admin_login.fxml", "Admin Login"); // Requires modification to handle scene closing/showing
    }

    @FXML
    private void handleLogout() {
        loadWelcomeScreen();
    }

    // loadScene is less useful now we are switching scenes on the same stage
    /*
    private void loadScene(String fxmlFile, String title) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/com/example/airline/view/" + fxmlFile));
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.setScene(new Scene(root));
        stage.show();
        // Close the current admin approval scene
        ((Stage) approveButton.getScene().getWindow()).close();
    }
    */

    private void loadWelcomeScreen() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/example/airline/view/welcome.fxml"));
            Stage stage = (Stage) logoutButton.getScene().getWindow(); // Use any button on the current scene
            stage.setScene(new Scene(root));
            stage.setTitle("Welcome");
            stage.show();
        } catch (IOException e) {
            System.err.println("Error loading welcome screen: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Logout Error", "Failed to load welcome screen.");
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}