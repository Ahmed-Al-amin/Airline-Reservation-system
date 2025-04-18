package com.example.airline.controller;

import com.example.airline.model.entity.User;
import com.example.airline.model.service.UserService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class AdminUserManagementController {

    @FXML private TableView<User> usersTableView;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, String> roleColumn;
    @FXML private TableColumn<User, Boolean> approvedColumn;
    @FXML private TableColumn<User, String> emailColumn;
    @FXML private TableColumn<User, String> phoneColumn;
    @FXML private Button toggleApprovalButton;
    @FXML private Button deleteUserButton;
    @FXML private Button backButton;
    @FXML private Label infoLabel;

    private User loggedInUser; // Should be the root admin
    private final UserService userService = new UserService();
    private ObservableList<User> allUsersData = FXCollections.observableArrayList();

    public void setLoggedInUser(User user) {
        this.loggedInUser = user;
        if (loggedInUser == null || !"admin".equalsIgnoreCase(loggedInUser.getUsername())) {
            // Should not happen if navigation is correct, but as a safeguard
            infoLabel.setText("Error: Access restricted.");
            usersTableView.setDisable(true);
            toggleApprovalButton.setDisable(true);
            deleteUserButton.setDisable(true);
        }
    }

    @FXML
    public void initialize() {
        setupTableColumns();

        // Bind the sorted list to the TableView
        SortedList<User> sortedData = new SortedList<>(allUsersData);
        sortedData.comparatorProperty().bind(usersTableView.comparatorProperty());
        usersTableView.setItems(sortedData);

        // Listener to enable/disable buttons based on selection and role
        usersTableView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    boolean isSelected = (newSelection != null);
                    boolean isRootAdminSelected = isSelected && "admin".equalsIgnoreCase(newSelection.getUsername());
                    boolean isRegularAdminSelected = isSelected && "admin".equals(newSelection.getRole()) && !isRootAdminSelected;

                    deleteUserButton.setDisable(!isSelected || isRootAdminSelected); // Cannot delete null or root admin
                    toggleApprovalButton.setDisable(!isRegularAdminSelected); // Can only toggle approval for non-root admins

                    // Update toggle button text
                    if (isRegularAdminSelected) {
                        toggleApprovalButton.setText(newSelection.isApproved() ? "Disapprove Admin" : "Approve Admin");
                    } else {
                        toggleApprovalButton.setText("Approve/Disapprove"); // Default text when disabled or passenger selected
                    }
                }
        );
        // Initially disable buttons
        deleteUserButton.setDisable(true);
        toggleApprovalButton.setDisable(true);
        infoLabel.setText("Select a user to manage.");
    }

    private void setupTableColumns() {
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phoneNumber")); // Match getter name

        // Custom cell factory for boolean 'approved' column to show Yes/No
        approvedColumn.setCellValueFactory(new PropertyValueFactory<>("approved"));
        approvedColumn.setCellFactory(col -> new TableCell<User, Boolean>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : (item ? "Yes" : "No"));
                // Optionally add styling for role='admin' and approved=false
                // if (!empty && getTableRow() != null && getTableRow().getItem() != null) {
                //     User user = getTableRow().getItem();
                //     if ("admin".equals(user.getRole()) && !item) {
                //         setStyle("-fx-text-fill: orange;"); // Example style
                //     } else {
                //         setStyle("");
                //     }
                // } else {
                //     setStyle("");
                // }
            }
        });

        // Default sort
        usernameColumn.setSortType(TableColumn.SortType.ASCENDING);
        usersTableView.getSortOrder().add(usernameColumn);
    }

    /** Loads all users into the table */
    public void loadAllUsers() {
        System.out.println("Root Admin loading all users...");
        List<User> users = userService.getAllUsers();
        allUsersData.setAll(users);
        usersTableView.sort(); // Apply default sort

        infoLabel.setText(users.isEmpty() ? "No users found in the system." : "Select a user to manage.");
        System.out.println("Root Admin displayed " + users.size() + " users.");
    }

    @FXML
    private void handleToggleApproval() {
        User selectedUser = usersTableView.getSelectionModel().getSelectedItem();

        if (selectedUser == null || !"admin".equals(selectedUser.getRole()) || "admin".equalsIgnoreCase(selectedUser.getUsername())) {
            showAlert(Alert.AlertType.WARNING, "Invalid Selection", "Please select a regular admin user to approve or disapprove.");
            return;
        }

        boolean success;
        String actionText;
        if (selectedUser.isApproved()) {
            // Currently approved, so disapprove
            actionText = "disapprove";
            success = userService.disapproveAdmin(selectedUser.getUsername());
        } else {
            // Currently disapproved, so approve
            actionText = "approve";
            success = userService.approveAdmin(selectedUser.getUsername());
        }

        if (success) {
            showAlert(Alert.AlertType.INFORMATION, "Success", "Admin '" + selectedUser.getUsername() + "' has been " + (actionText.equals("approve") ? "approved." : "disapproved."));
            // Refresh the specific user data in the list for immediate UI update
            refreshUserData(selectedUser);
        } else {
            showAlert(Alert.AlertType.ERROR, "Operation Failed", "Could not " + actionText + " admin '" + selectedUser.getUsername() + "'.");
        }
    }

    @FXML
    private void handleDeleteUser() {
        User selectedUser = usersTableView.getSelectionModel().getSelectedItem();

        if (selectedUser == null) {
            showAlert(Alert.AlertType.WARNING, "Selection Required", "Please select a user to delete.");
            return;
        }
        if ("admin".equalsIgnoreCase(selectedUser.getUsername())) {
            showAlert(Alert.AlertType.ERROR, "Action Denied", "The root admin account cannot be deleted.");
            return;
        }
        if (selectedUser.getUsername().equalsIgnoreCase(loggedInUser.getUsername())) {
            showAlert(Alert.AlertType.ERROR, "Action Denied", "You cannot delete your own account.");
            return;
        }

        // Confirmation Dialog
        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("Confirm Deletion");
        confirmationAlert.setHeaderText("Delete User: " + selectedUser.getUsername() + " (Role: " + selectedUser.getRole() + ")");
        confirmationAlert.setContentText("Are you sure you want to permanently delete this user?\n" +
                "This action cannot be undone. Associated reservations might be affected (username set to NULL).");

        Optional<ButtonType> result = confirmationAlert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean success = userService.deleteUser(selectedUser.getUsername(), loggedInUser.getUsername());
            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Deletion Successful", "User '" + selectedUser.getUsername() + "' has been deleted.");
                // Remove from the underlying list, which updates the TableView
                allUsersData.remove(selectedUser);
            } else {
                showAlert(Alert.AlertType.ERROR, "Deletion Failed", "Could not delete user '" + selectedUser.getUsername() + "'. See logs for details.");
            }
        } else {
            System.out.println("User deletion cancelled by admin.");
        }
    }

    @FXML
    private void handleBack() {
        // Navigate back to the screen the root admin came from (likely Admin Approval)
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/airline/view/admin_approval.fxml"));
            Parent root = loader.load();
            AdminApprovalController controller = loader.getController();
            if(controller != null && loggedInUser != null) {
                controller.setLoggedInUser(loggedInUser);
                controller.loadPendingAdmins(); // Reload pending admins
            }

            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Admin Approval");
        } catch (IOException | IllegalStateException e) {
            System.err.println("Error loading Admin Approval screen: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not load the admin approval screen.");
        }
    }

    /** Refreshes the data for a specific user in the table */
    private void refreshUserData(User updatedUser) {
        // Find the index of the user and update it to trigger TableView refresh
        for(int i=0; i < allUsersData.size(); i++) {
            if(allUsersData.get(i).getUsername().equals(updatedUser.getUsername())) {
                // Fetch the very latest state from DB after update
                User freshUser = userService.getUserByUsername(updatedUser.getUsername());
                if(freshUser != null) {
                    allUsersData.set(i, freshUser);
                } else {
                    // User might have been deleted concurrently? Remove from list.
                    allUsersData.remove(i);
                }
                break;
            }
        }
        // Force redraw/re-sort might be needed sometimes
        usersTableView.refresh();
        usersTableView.sort();

    }


    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}