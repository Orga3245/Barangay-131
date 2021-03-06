package javah;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;

public class Main extends Application {

    private static Stage PRIMARY_STAGE;

    public static String PHOTO_DIR_PATH, SIGNATURE_DIR_PATH, APP_DATA_PATH;

    @Override
    public void start(Stage primaryStage) throws Exception {
        PRIMARY_STAGE = primaryStage;
        // Initialize the javah window.
        FXMLLoader mainFxmlLoader = new FXMLLoader(getClass().getClassLoader().getResource("fxml/scene_main.fxml"));
        Scene mainScene = new Scene(mainFxmlLoader.load());

        // Initialize the primary stage containing the javah scene.
        primaryStage.setScene(mainScene);
        primaryStage.setMaximized(true);
        primaryStage.show();

        // Terminate the application when the x button is pressed.
        primaryStage.setOnCloseRequest(event -> System.exit(0));
    }

    /**
     * Fetch the primary stage.
     *
     * @return the primary stage.
     */
    public static Stage getPrimaryStage() {
        return PRIMARY_STAGE;
    }

    public static void main(String[] args) {
        // Make the directories to store the images needed. Make path towards the root
        // folder 'Barangay131' at C:\Users\Public and its sub-folders - 'Photos' and
        // 'Signature'.
        String dataDirectoryPath = System.getenv("PUBLIC") + "/Barangay131";

        APP_DATA_PATH = dataDirectoryPath;
        PHOTO_DIR_PATH = dataDirectoryPath + "/Photos";
        SIGNATURE_DIR_PATH = dataDirectoryPath + "/Signatures";

        File appDataDirectory = new File(APP_DATA_PATH);
        if(!appDataDirectory.exists())
            appDataDirectory.mkdir();

        // Create the directories if not yet created.
        File photoDirectory = new File(PHOTO_DIR_PATH);
        if(!photoDirectory.exists())
            photoDirectory.mkdir();

        File signatureDirectory = new File(SIGNATURE_DIR_PATH);
        if(!signatureDirectory.exists())
            signatureDirectory.mkdir();


        launch(args);
    }
}