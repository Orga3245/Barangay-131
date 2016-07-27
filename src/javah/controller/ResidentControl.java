package javah.controller;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Paint;
import javah.DatabaseControl;
import javah.container.Resident;
import javah.util.ListFilter;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class ResidentControl {

    /**
     * Used for resident list paging. *Each page contains 40 residents.
     */
    @FXML private GridPane mResidentListGridPane;

    /**
     * The current page label of the resident list paging.
     */
    @FXML private Label mCurrentPageLabel;

    /**
     * The total number of pages of the resident list paging.
     */
    @FXML private Label mPageCountLabel;

    /**
     * The search field used for specialized resident query.
     */
    @FXML private TextField mSearchField;

    /**
     * Widgets representing the information of the selected resident.
     */
    @FXML private ImageView mResidentPhoto;
    @FXML private Label mResidentName, mBirthDate, mAge, mResidentSince, mAddress2Label;
    @FXML private TextArea mAddress1, mAddress2;

    /**
     * Edit or manipulate the currently selected resident with this buttons.
     */
    @FXML private ImageView mEditButton, mDeleteButton;

    private DatabaseControl mDatabaseControl;

    /**
     * The list containing all the non-archived resident IDs.
     */
    private List<String> mResidentIDsCache;

    /**
     * The list containing all the non-archived resident names in proper format.
     */
    private List<String> mResidentNamesCache;

    /**
     * A volatile copy of the mResidentIDsCache used to search for non-archived residents.
     */
    private List<String> mResidentIDs;

    /**
     * A volatile copy of the mResidentNamesCache used to display the residents in the list paging.
     */
    private List<String> mResidentNames;

    /**
     * The newResidentSelectedIndex of the selected resident. *Default value is -1, meaning no resident selected.
     */
    private int mResidentSelectedIndex = -1;

    /**
     * The array containing all the labels of the resident list paging.
     */
    private Label[] mResidentLabels;

    /**
     * Represents the current page of the resident list paging.
     */
    private int mCurrentPage;

    /**
     * Represents the number of pages within the resident list paging.
     */
    private int mPageCount;

    /**
     * Represents the total number of residents within the resident list paging.
     * *Does not reflect cached residents.
     */
    private int mResidentCount;

    @FXML
    private void initialize() {
        // Initialize the db controller.
        mDatabaseControl = new DatabaseControl();

        // Cache resident IDs and names from the database.
        List[] lists = mDatabaseControl.getResidentsIdAndName();
        mResidentIDsCache = lists[0];
        mResidentNamesCache = lists[1];

        // Create a volatile copy of the cached data.
        mResidentIDs = mResidentIDsCache;
        mResidentNames = mResidentNamesCache;

        // Initialize mResidentLabels with storage for 40 labels.
        mResidentLabels = new Label[40];

        // Populate mResidentLabels with 40 labels and display it in a matrix of 20x2 mResidentListGridPane.
        for (int i = 0; i < 40; i++) {
            Label label = new Label();
            label.setStyle("-fx-background-color: f4f4f4;" + "-fx-font-size: 20;");
            label.setAlignment(Pos.CENTER);
            label.setPrefHeight(500);
            label.setPrefWidth(1000);

            mResidentLabels[i] = label;
            mResidentListGridPane.add(label, i / 20, i >= 20 ? i - 20 : i);

            // Add a label selected event listener to each label.
            final int newResidentSelectedIndex = i;
            label.setOnMouseClicked(event -> setResidentSelected(newResidentSelectedIndex));

        }

        // Determine the initial number of Pages and set the default current page to 1.
        mResidentCount = mResidentIDs.size();
        mCurrentPage = 1;
        mPageCount = (int) Math.ceil(mResidentCount / 40.0);

        mCurrentPageLabel.setText(mCurrentPage + "");
        mPageCountLabel.setText(mPageCount + "");

        updateCurrentPage();
    }

    private void setResidentSelected(int newResidentSelectedIndex) {
        Consumer<Boolean> setResidentInfoDisplayed = (isDisplayed) -> {
            if (isDisplayed) {
                // Query the data of the currently selected resident.
                Resident resident = mDatabaseControl.getResident(mResidentIDs.get(mResidentSelectedIndex));

                mResidentPhoto.setImage(new Image(resident.getPhotoPath() != null ?
                        "file:" + resident.getPhotoPath() : "/res/ic_default_resident.png"));

                mResidentName.setText(mResidentNames.get(mResidentSelectedIndex));

                // Format birthdate to YYYY dd, mm
                // Set the displayed birth date.
                LocalDate birthDate = resident.getBirthDate().toLocalDate();
                int birthYear = birthDate.getYear();
                int birthDay = birthDate.getDayOfMonth();

                String birthMonth = "January";
                switch(birthDate.getMonthValue()) {
                    case 2 : birthMonth = "February"; break;
                    case 3 : birthMonth = "March"; break;
                    case 4 : birthMonth = "April"; break;
                    case 5 : birthMonth = "May"; break;
                    case 6 : birthMonth = "June"; break;
                    case 7 : birthMonth = "July"; break;
                    case 8 : birthMonth = "August"; break;
                    case 9 : birthMonth = "September"; break;
                    case 10 : birthMonth = "October"; break;
                    case 11 : birthMonth = "November"; break;
                    case 12 : birthMonth = "December";
                }

                mBirthDate.setText("");
                mBirthDate.setText(String.format("%s %s, %s", birthMonth, birthDay, birthYear));

                // Set the displayed age.
                int age = Calendar.getInstance().get(Calendar.YEAR) - birthYear;

                if(birthYear != Calendar.getInstance().get(Calendar.YEAR))
                    age -= birthDate.getMonthValue() > Calendar.getInstance().get(Calendar.MONTH) ||
                            (birthDate.getMonthValue() == Calendar.getInstance().get(Calendar.MONTH) &&
                                    birthDate.getDayOfMonth() > Calendar.getInstance().get(Calendar.DAY_OF_MONTH)) ? 1 : 0;

                mAge.setText(age + "");

                // Set the displayed residency year.
                mResidentSince.setText(resident.getResidentSince() == -1 ? "Birth" : resident.getResidentSince() + "");


                // Set the displayed address 1.
                mAddress1.setText(resident.getAddress1());

                // Set the displayed address 2.
                if(resident.getAddress2() != null) {
                    mAddress2.setVisible(true);
                    mAddress2Label.setVisible(true);
                    mAddress2.setText(resident.getAddress2());
                } else {
                    mAddress2.setVisible(false);
                    mAddress2Label.setVisible(false);
                }

                mDeleteButton.setVisible(true);
                mEditButton.setVisible(true);

            } else {
                mDeleteButton.setVisible(false);
                mEditButton.setVisible(false);
                mAddress2.setVisible(false);
                mAddress2Label.setVisible(false);

                mResidentPhoto.setImage(new Image("/res/ic_default_resident.png"));
                mResidentName.setText("Exempli, Gratia E.G.");
                mBirthDate.setText("MMMM DD, YYYY");
                mAge.setText("---");
                mResidentSince.setText("YYYY or Birth");

                mAddress1.setText("Street Address, Neighborhood, City, State");
            }
        };

        if (mResidentSelectedIndex == -1) {
            if (newResidentSelectedIndex != -1) {
                mResidentLabels[newResidentSelectedIndex]
                        .setStyle("-fx-background-color: #0080FF;" + "-fx-font-size: 20;" + "-fx-text-fill: white");
                mResidentSelectedIndex = newResidentSelectedIndex;
                setResidentInfoDisplayed.accept(true);
            }

        } else {
            if (newResidentSelectedIndex == -1) {
                mResidentLabels[newResidentSelectedIndex]
                        .setStyle("-fx-background-color: #f4f4f4;" + "-fx-font-size: 20;" + "-fx-text-fill: black");
                mResidentSelectedIndex = -1;
                setResidentInfoDisplayed.accept(false);

            } else if (mResidentSelectedIndex == newResidentSelectedIndex) {
                mResidentLabels[newResidentSelectedIndex]
                        .setStyle("-fx-background-color: #f4f4f4;" + "-fx-font-size: 20;" + "-fx-text-fill: black");
                mResidentSelectedIndex = -1;
                setResidentInfoDisplayed.accept(false);

            } else {
                mResidentLabels[mResidentSelectedIndex]
                        .setStyle("-fx-background-color: #f4f4f4;" + "-fx-font-size: 20;" + "-fx-text-fill: black");

                mResidentLabels[newResidentSelectedIndex]
                        .setStyle("-fx-background-color: #0080FF;" + "-fx-font-size: 20;" + "-fx-text-fill: white");

                mResidentSelectedIndex = newResidentSelectedIndex;
                setResidentInfoDisplayed.accept(true);
            }
        }
    }

    /**
     * Update current page with respect to mCurrentPage. That is, the value of mCurrentPage will determine the displayed
     * residents.
     */
    private void updateCurrentPage() {
        int firstIndex = (mCurrentPage - 1) * 40;
        int lastIndex = mCurrentPage * 40 > mResidentCount - 1 ? mResidentCount - 1 : mCurrentPage * 40;
        int currentIndex = firstIndex;

        for (int i = 0; i < 40; i++) {
            if (currentIndex <= lastIndex) {
                mResidentLabels[i].setText(mResidentNames.get(currentIndex));
                currentIndex++;
            } else
                mResidentLabels[i].setText("");
        }
    }

    /**
     * Update the resident list paging to display the residents that has a match with the text in the search field.
     * A black search field will result to displaying all the residents.
     * @param event
     */
    @FXML
    public void onSearchButtonClicked(Event event) {
        String keywords = mSearchField.getText();

        if (keywords.trim().equals("")) {
            mResidentIDs = mResidentIDsCache;
            mResidentNames = mResidentNamesCache;
        } else {
            String[] keywordsArray = keywords.split(" ");

            List[] lists = ListFilter.filterLists(mResidentIDsCache, mResidentNamesCache, keywordsArray);


            mResidentIDs = lists[0];
            mResidentNames = lists[1];
        }

        mResidentCount = mResidentIDs.size();
        mCurrentPage = 1;
        mPageCount = (int) Math.ceil(mResidentCount / 40.0);

        mCurrentPageLabel.setText(mCurrentPage + "");
        mPageCountLabel.setText(mPageCount + "");

        updateCurrentPage();
    }

    /**
     * If the Enter key is pressed within the search field, then automatically click the search button.
     * @param event
     */
    @FXML
    public void onSearchFieldKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            onSearchButtonClicked(null);
            mCurrentPageLabel.requestFocus();
        }
    }

    /**
     * Move the resident list paging to the previous page when possible.
     * @param event
     */
    @FXML
    public void onBackPageButtonClicked(Event event) {
        if (mCurrentPage > 1) {
            mCurrentPage -= 1;
            updateCurrentPage();
            mCurrentPageLabel.setText(mCurrentPage + "");
        }
    }

    /**
     * Move the resident list paging to the next page when possible.
     * @param event
     */
    @FXML
    public void onNextPageButtonClicked(Event event) {
        if(mCurrentPage < mPageCount) {
            mCurrentPage += 1;
            updateCurrentPage();
            mCurrentPageLabel.setText(mCurrentPage + "");
        }
    }



    @FXML
    public void onNewResidentButtonClicked(ActionEvent actionEvent) {

    }

    @FXML
    public void onEditResidentButtonClicked(Event event) {
    }

    @FXML
    public void onDeleteResidentButtonClicked(Event event) {
    }


}
