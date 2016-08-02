package javah.controller.resident;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javah.container.Resident;
import javah.model.CacheModel;
import javah.model.DatabaseModel;
import javah.util.ListFilter;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class ResidentControl {

    /**
     * An interface that tells the main scene to open up a manipulation resident dialog.
     */
    public interface OnResidentSceneListener {
        void onNewResidentButtonClicked();
        void onEditResidentButtonClicked(Resident resident);
        void onDeleteResidentButtonClicked(Resident resident);
    }

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

    private DatabaseModel mDatabaseModel;

    private CacheModel mCacheModel;

    /**
     * A volatile copy of the mResidentIDsCache used to search for non-archived residents.
     */
    private List<String> mResidentIDs;

    /**
     * A volatile copy of the mResidentNamesCache used to display the residents in the list paging.
     */
    private List<String> mResidentNames;

    /**
     * The value representing which label is selected from the resident list paging.
     * Value range is between 0 - 39.
     */
    private int mLabelSelectedIndex;

    /**
     * The value representing the index of the selected resident.
     * Value range is between 0 - [mResidentIDs.size() - 1].
     */
    private int mResidentSelectedIndex;

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

    private Resident mResidentSelected;

    private OnResidentSceneListener mListener;

    /**
     * Called before setCacheModel()
     */
    @FXML
    private void initialize() {

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
            final int labelIndex = i;
            label.setOnMouseClicked(event -> setResidentSelected(labelIndex));
        }
    }

    /**
     * Update the resident list paging to display the residents that has a match with the text in the search field.
     * A blank search field will result to displaying all the residents.
     * @param event
     */
    @FXML
    public void onSearchButtonClicked(Event event) {
        String keywords = mSearchField.getText();

        if (keywords.trim().equals("")) {
            mResidentIDs = mCacheModel.getResidentIDsCache();
            mResidentNames = mCacheModel.getmResidentNamesCache();
        } else {
            String[] keywordsArray = keywords.split(" ");

            List[] lists = ListFilter.filterLists(
                    mCacheModel.getResidentIDsCache(), mCacheModel.getmResidentNamesCache(), keywordsArray);


            mResidentIDs = lists[0];
            mResidentNames = lists[1];
        }

        updateListPaging(false);
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

    /**
     * Tell the main scene to show the resident creation dialog.
     * @param actionEvent
     */
    @FXML
    public void onNewResidentButtonClicked(ActionEvent actionEvent) {
        mListener.onNewResidentButtonClicked();
    }

    /**
     * Tell the main scenne to show the resident update dialog.
     * @param event
     */
    @FXML
    public void onEditResidentButtonClicked(Event event) {
        mListener.onEditResidentButtonClicked(mResidentSelected);
    }

    /**
     * Tell the main scene to show the resident delete confirmation dialog.
     * @param event
     */
    @FXML
    public void onDeleteResidentButtonClicked(Event event) {
        mListener.onDeleteResidentButtonClicked(mResidentSelected);
    }

    /**
     * Called after initialize() and is called in the MainControl.
     * Set the main scene as the listener to this object.
     * @param listener
     */
    public void setListener(OnResidentSceneListener listener) {
        mListener = listener;
    }

    /**
     * Called after initialize() and is called in the MainControl.
     * Make a reference to the global database model.
     * @param databaseModel
     */
    public void setDatabaseModel(DatabaseModel databaseModel) {
        mDatabaseModel = databaseModel;
    }

    /**
     * Called after initialize() and is called in the MainControl.
     * Make a reference to the global cache model and start initializing the variables that are initially
     * dependent on some cached data.
     * @param cacheModel
     */
    public void setCacheModel(CacheModel cacheModel) {

        mCacheModel = cacheModel;
        // Create a volatile copy of the cached data.
        mResidentIDs = mCacheModel.getResidentIDsCache();
        mResidentNames = mCacheModel.getmResidentNamesCache();

        // Determine the initial number of Pages and set the default current page to 1.
        updateListPaging(false);
    }

    public void deleteSelectedResident() {
        mDatabaseModel.archiveResident(mResidentSelected.getId());
        mResidentIDs.remove(mResidentSelectedIndex);
        mResidentNames.remove(mResidentSelectedIndex);

        updateListPaging(true);
    }

    public void createResident(Resident resident) {
        // Create the resident and get its corresponding unique id.
        String residentId = mDatabaseModel.createResident(resident);
        System.out.println("Resident id: " + residentId);

        // Format the resident name to be inserted in the list.
        String residentName = String.format("%s, %s %s.",
                resident.getLastName(),
                resident.getFirstName(),
                resident.getMiddleName().toUpperCase().charAt(0));

        // Add the resident name to the resident names list and then sort it alphabetically.
        mResidentNames.add(residentName);
        Collections.sort(mResidentNames, String.CASE_INSENSITIVE_ORDER);

        // Get the index of the resident name within the list after insertion.
        int index = mResidentNames.indexOf(residentName);

        // Use the acquired index to insert the resident ID to the resident IDs list.
        mResidentIDs.add(index, residentId);

        // Once the resident is created, the current page must be placed where the newly created resident is inserted and
        // must be auto selected.
        mResidentCount = mResidentIDs.size();
        mPageCount = (int) Math.ceil(mResidentCount / 40.0);
        mCurrentPage = (int) Math.ceil(index / 39.0);

        mCurrentPageLabel.setText(mCurrentPage + "");
        mPageCountLabel.setText(mPageCount + "");

        // Display the default data when no resident is selected.
        updateCurrentPage();

        setResidentSelected(index % 40);
    }

    public void setBlurListPaging(boolean blur) {
        if (blur) {
            mResidentListGridPane.setStyle("-fx-background-color: #f4f4f4;");
        } else {
            mResidentListGridPane.setStyle("-fx-background-color: #000000;" + "-fx-hgap: 1;" + "-fx-vgap: 1;" + "-fx-padding: 1;");

        }
    }

    /**
     * Update the resident selected data displayed.
     * @param newLabelSelectedIndex is the index of the label containing the resident to be displayed. If it is equal
     *                              to -1, then the example data is displayed.
     */
    private void setResidentSelected(int newLabelSelectedIndex) {

        // Determine the index of the resident in place of the currently selected label.
        mResidentSelectedIndex = newLabelSelectedIndex + 40 * (mCurrentPage - 1);

        /**
         * If a resident is selected, then display its data.
         * If a resident is unselected or no resident is selected, then display the example data.
         */
        Consumer<Boolean> setDisplaySelectedResidentInfo = (isDisplayed) -> {

            Function<Integer, String> convertMonthIntToString = (monthValue) -> {
                String birthMonth = "January";
                switch(monthValue) {
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

                return birthMonth;
            };

            if (isDisplayed) {

                // Query the data of the currently selected resident.
                mResidentSelected = mDatabaseModel.getResident(mResidentIDs.get(mResidentSelectedIndex));

                mResidentPhoto.setImage(new Image(mResidentSelected.getPhotoPath() != null ?
                        "file:" + mResidentSelected.getPhotoPath() : "/res/ic_default_resident.png"));

                mResidentName.setText(mResidentNames.get(mResidentSelectedIndex));

                // Format birthdate to YYYY dd, mm
                // Set the displayed birth date.
                LocalDate birthDate = mResidentSelected.getBirthDate().toLocalDate();
                int birthYear = birthDate.getYear();
                int birthDay = birthDate.getDayOfMonth();
                String birthMonth = convertMonthIntToString.apply(birthDate.getMonthValue());

                mBirthDate.setText("");
                mBirthDate.setText(String.format("%s %s, %s", birthMonth, birthDay, birthYear));

                // Set the displayed age.
                int age = Calendar.getInstance().get(Calendar.YEAR) - birthYear;

                if(birthYear != Calendar.getInstance().get(Calendar.YEAR))
                    age -= birthDate.getMonthValue() > Calendar.getInstance().get(Calendar.MONTH) ||
                            (birthDate.getMonthValue() == Calendar.getInstance().get(Calendar.MONTH) &&
                                    birthDate.getDayOfMonth() > Calendar.getInstance().get(Calendar.DAY_OF_MONTH)) ? 1 : 0;

                mAge.setText(age + "");

                // Set the displayed year and month of residency.
                mResidentSince.setText(
                        mResidentSelected.getYearOfResidency() == -1 ?
                                "Birth" : convertMonthIntToString.apply(mResidentSelected.getMonthOfResidency()) + " " +
                                        mResidentSelected.getYearOfResidency());


                // Set the displayed address 1.
                mAddress1.setText(mResidentSelected.getAddress1());

                // Set the displayed address 2.
                if(mResidentSelected.getAddress2() != null) {
                    mAddress2.setVisible(true);
                    mAddress2Label.setVisible(true);
                    mAddress2.setText(mResidentSelected.getAddress2());
                } else {
                    mAddress2.setVisible(false);
                    mAddress2Label.setVisible(false);
                }

                mDeleteButton.setVisible(true);
                mEditButton.setVisible(true);

            } else {
                mResidentPhoto.setImage(new Image("/res/ic_default_resident.png"));
                mResidentName.setText("");
                mBirthDate.setText("");
                mAge.setText("");
                mResidentSince.setText("");

                mAddress1.setText("");
                mAddress2.setVisible(false);
                mAddress2Label.setVisible(false);

                mDeleteButton.setVisible(false);
                mEditButton.setVisible(false);
            }
        };

        // This is where the code selection and unselection view update happens.

        // If a label is clicked without containing any resident, then ignore the event.
        if (mResidentSelectedIndex < mResidentIDs.size()) {
            // if no previous resident is selected, then simply make the new selection.
            if (mLabelSelectedIndex == -1) {
                if (newLabelSelectedIndex != -1) {
                    mResidentLabels[newLabelSelectedIndex]
                            .setStyle("-fx-background-color: #0080FF;" + "-fx-font-size: 20;" + "-fx-text-fill: white");
                    mLabelSelectedIndex = newLabelSelectedIndex;
                    setDisplaySelectedResidentInfo.accept(true);
                }

            } else {
                // If there is a previous selection, unselect it.
                // Also, if the previously selected resident is selected again, then unselect it.
                if (newLabelSelectedIndex == -1 || mLabelSelectedIndex == newLabelSelectedIndex) {
                    mResidentLabels[mLabelSelectedIndex]
                            .setStyle("-fx-background-color: #f4f4f4;" + "-fx-font-size: 20;" + "-fx-text-fill: black");
                    mLabelSelectedIndex = -1;
                    mResidentSelectedIndex = -1;
                    setDisplaySelectedResidentInfo.accept(false);

                // Unselect the previously selcted resident, then select the currently selected resident.
                } else {
                    mResidentLabels[mLabelSelectedIndex]
                            .setStyle("-fx-background-color: #f4f4f4;" + "-fx-font-size: 20;" + "-fx-text-fill: black");

                    mResidentLabels[newLabelSelectedIndex]
                            .setStyle("-fx-background-color: #0080FF;" + "-fx-font-size: 20;" + "-fx-text-fill: white");

                    mLabelSelectedIndex = newLabelSelectedIndex;
                    setDisplaySelectedResidentInfo.accept(true);
                }
            }
        }
    }

    /**
     * Update current page with respect to mCurrentPage. That is, the value of mCurrentPage will determine the displayed
     * residents.
     * Moving from one page to another removes the resident selected.
     */
    private void updateCurrentPage() {
        // Make sure that no resident is selected when moving from one page to another.
        setResidentSelected(-1);

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
     * Updates the list paging, mResidentCount and mPageCount.
     * @param stayOnPage determines whether current page should be maintained or not after the update.
     */
    private void updateListPaging(boolean stayOnPage) {
        mResidentCount = mResidentIDs.size();
        mPageCount = (int) Math.ceil(mResidentCount / 40.0);
        mCurrentPage = stayOnPage ? (mPageCount < mCurrentPage) ? mCurrentPage-- : mCurrentPage : 1;;

        mCurrentPageLabel.setText(mCurrentPage + "");
        mPageCountLabel.setText(mPageCount + "");

        updateCurrentPage();
    }

}
