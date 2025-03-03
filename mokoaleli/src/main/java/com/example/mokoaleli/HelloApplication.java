package com.example.mokoaleli;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.control.ScrollPane;
import javafx.stage.Stage;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

public class HelloApplication extends Application {
    private File selectedFile;
    private Stage galleryStage;
    private ProgressIndicator loadingIndicator;
    private File[] currentFiles;
    private int currentIndex;

    @Override
    public void start(Stage primaryStage) {
        VBox mainContainer = new VBox(20);
        mainContainer.getStyleClass().add("main-container");

        Label title = new Label("Fashion Gallery");
        title.getStyleClass().add("main-title");

        HBox buttonContainer = new HBox(20);
        buttonContainer.setAlignment(Pos.CENTER);

        Button menButton = createStyledButton("Men", "category-button");
        Button womenButton = createStyledButton("Women", "category-button");
        Button kidsButton = createStyledButton("Kids", "category-button");

        menButton.setOnAction(e -> openGallery("men"));
        womenButton.setOnAction(e -> openGallery("women"));
        kidsButton.setOnAction(e -> openGallery("kids"));

        buttonContainer.getChildren().addAll(menButton, womenButton, kidsButton);
        mainContainer.getChildren().addAll(title, buttonContainer);

        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setVisible(false);
        loadingIndicator.getStyleClass().add("loading-indicator");
        mainContainer.getChildren().add(loadingIndicator);

        Scene scene = new Scene(mainContainer, 800, 600);
        scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());

        primaryStage.setTitle("Fashion Gallery");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Added missing method
    private Button createStyledButton(String text, String styleClass) {
        Button button = new Button(text);
        button.getStyleClass().add(styleClass);
        return button;
    }

    private void openGallery(String category) {
        loadingIndicator.setVisible(true);
        Task<Scene> loadTask = new Task<>() {
            @Override
            protected Scene call() {
                return createGalleryScene(category);
            }
        };
        loadTask.setOnSucceeded(e -> {
            loadingIndicator.setVisible(false);
            Platform.runLater(() -> {  // Added Platform.runLater() for UI updates
                if (galleryStage == null) galleryStage = new Stage();
                galleryStage.setScene(loadTask.getValue());
                galleryStage.setTitle(category.substring(0, 1).toUpperCase() + category.substring(1) + " Gallery");
                galleryStage.show();
            });
        });
        new Thread(loadTask).start();
    }

    private Scene createGalleryScene(String category) {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("gallery-grid");
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setPadding(new Insets(20));

        ScrollPane scrollPane = new ScrollPane(grid);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("gallery-scroll-pane");

        File folder = new File("C:/Users/Administrator/IdeaProjects/demo2/target/classes/images/" +
                (category.equals("women") ? "ladies" : category));
        if (!folder.exists() || !folder.isDirectory()) return createErrorScene(category);

        currentFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".jpeg"));
        if (currentFiles == null) return new Scene(grid, 800, 600);
        Arrays.sort(currentFiles);

        int col = 0, row = 0;
        for (File file : currentFiles) {
            try {
                ImageView imageView = createThumbnail(file);
                int finalIndex = Arrays.asList(currentFiles).indexOf(file);  // Store index in final variable
                imageView.setOnMouseClicked(e -> {
                    currentIndex = finalIndex;  // Use the stored index
                    showFullImage(file, category);
                });

                // Add container for image
                StackPane imageContainer = new StackPane(imageView);
                imageContainer.getStyleClass().add("image-container");

                grid.add(imageContainer, col, row);
                col++;
                if (col > 2) {
                    col = 0;
                    row++;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        VBox mainLayout = new VBox(10);
        Button backButton = createStyledButton("main menu", "navigation-button");
        backButton.setOnAction(e -> galleryStage.close());
        mainLayout.getChildren().addAll(scrollPane, backButton);
        mainLayout.setAlignment(Pos.CENTER);

        Scene scene = new Scene(mainLayout, 800, 600);
        scene.getStylesheets().add(getClass().getResource("/styles/gallery.css").toExternalForm());
        return scene;
    }

    private ImageView createThumbnail(File file) throws IOException {
        Image image = new Image(new FileInputStream(file));
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(200);
        imageView.setFitHeight(200);
        imageView.setPreserveRatio(true);
        imageView.getStyleClass().add("thumbnail");
        return imageView;
    }

    private void showFullImage(File file, String category) {
        selectedFile = file;
        try {
            Image image = new Image(new FileInputStream(file));
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(400);
            imageView.setFitHeight(400);  // Added missing height constraint
            imageView.setPreserveRatio(true);

            Button prevButton = createStyledButton("Previous", "nav-button");
            Button nextButton = createStyledButton("Next", "nav-button");
            Button backButton = createStyledButton("Back", "nav-button");

            // Added download button
            Button downloadButton = createStyledButton("Download", "download-button");
            downloadButton.setOnAction(e -> downloadImage());

            backButton.setOnAction(e -> {
                Scene galleryScene = createGalleryScene(category);
                Platform.runLater(() -> galleryStage.setScene(galleryScene));
            });

            prevButton.setOnAction(e -> navigateImage(-1, category));
            nextButton.setOnAction(e -> navigateImage(1, category));

            HBox navBox = new HBox(10, prevButton, nextButton, backButton, downloadButton);
            navBox.setAlignment(Pos.CENTER);

            VBox layout = new VBox(20, imageView, navBox);
            layout.setAlignment(Pos.CENTER);

            Scene scene = new Scene(layout, 800, 800);
            scene.getStylesheets().add(getClass().getResource("/styles/gallery.css").toExternalForm());
            Platform.runLater(() -> galleryStage.setScene(scene));  // Added Platform.runLater()
        } catch (IOException e) {
            showError("Error loading image");
        }
    }

    private void navigateImage(int step, String category) {
        int newIndex = currentIndex + step;
        if (newIndex >= 0 && newIndex < currentFiles.length) {
            currentIndex = newIndex;
            showFullImage(currentFiles[currentIndex], category);
        }
    }

    // Added missing error handling methods
    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    // Added missing method for error scene
    private Scene createErrorScene(String category) {
        VBox errorContainer = new VBox(20);
        errorContainer.getStyleClass().add("error-container");

        Label errorMessage = new Label("The folder for " + category + " does not exist.");
        errorMessage.getStyleClass().add("error-message");

        Button backButton = createStyledButton("Back", "navigation-button");
        backButton.setOnAction(e -> galleryStage.close());

        errorContainer.getChildren().addAll(errorMessage, backButton);
        errorContainer.setAlignment(Pos.CENTER);

        Scene errorScene = new Scene(errorContainer, 800, 600);
        errorScene.getStylesheets().add(getClass().getResource("/styles/gallery.css").toExternalForm());
        return errorScene;
    }

    // Added missing download method
    private void downloadImage() {
        if (selectedFile != null) {
            File downloadDir = new File("downloads");
            if (!downloadDir.exists()) {
                downloadDir.mkdir();
            }

            File dest = new File("downloads/" + selectedFile.getName());
            try {
                Files.copy(selectedFile.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                showInfo("Download Complete", "Image saved to: " + dest.getAbsolutePath());
            } catch (IOException e) {
                showError("Failed to download image");
            }
        }
    }

    // Added missing info dialog method
    private void showInfo(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}