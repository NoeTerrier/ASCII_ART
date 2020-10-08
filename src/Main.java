import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.converter.NumberStringConverter;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.UnaryOperator;

public class Main extends Application {

    private static final String TITLE = "ASCII art";
    private static final String AWESOME_FONT_PATH = "/Font Awesome 5 Free-Solid-900.otf";
    private static final String STYLE_PATH = "/style/stylesheet.css";
    private static final int STAGE_WIDTH = 800;
    private static final int STAGE_HEIGHT = 800;
    private static final Font AWESOME_FONT = loadFont();

    private static final FileChooser fileChooser = new FileChooser();
    private final ImageASCIIJFX imageASCIIJFX = new ImageASCIIJFX();
    private final ObjectProperty<Image> imageRefProperty = new SimpleObjectProperty<>();
    private final ObjectProperty<Image> imageASCIIProperty = new SimpleObjectProperty<>();

    private final IntegerProperty heightProperty = new SimpleIntegerProperty(100);
    private final IntegerProperty widthProperty = new SimpleIntegerProperty(100);
    private final IntegerProperty pageHeightProperty = new SimpleIntegerProperty(2000);
    private final IntegerProperty pageWidthProperty = new SimpleIntegerProperty(2000);

    private Stage stage;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;

        //file extension openable
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        imageASCIIJFX.widthProperty().bind(widthProperty);
        imageASCIIJFX.heightProperty().bind(heightProperty);

        imageASCIIJFX.pageWidthProperty().bindBidirectional(pageWidthProperty);
        imageASCIIJFX.pageHeightProperty().bindBidirectional(pageHeightProperty);

        imageASCIIJFX.imageRefProperty().bind(Bindings.createObjectBinding(() ->
                        (imageRefProperty.get() != null)
                                ? SwingFXUtils.fromFXImage(imageRefProperty.get(), null)
                                : null
                , imageRefProperty));

        imageASCIIProperty.bind(Bindings.createObjectBinding(() ->
                        (imageASCIIJFX.getImageASCII() != null)
                                ? SwingFXUtils.toFXImage(imageASCIIJFX.getImageASCII(), null)
                                : null
                , imageRefProperty, imageASCIIJFX.imageASCIIProperty()));

        SplitPane splitPane = new SplitPane(createImgViewPane(imageRefProperty), createImgViewPane(imageASCIIProperty));
        splitPane.setFocusTraversable(false);

        Scene scene = new Scene(new BorderPane(splitPane, getControlBar(), null, null, null));
        scene.getStylesheets().add(STYLE_PATH);

        primaryStage.setTitle(TITLE);
        primaryStage.setMinWidth(STAGE_WIDTH);
        primaryStage.setMinHeight(STAGE_HEIGHT);
        primaryStage.setScene(scene);

        primaryStage.show();
    }

    /**
     * return control bar with all parameter's textfield and button
     * @return control bar with all parameter's textfield and buttons
     */
    private HBox getControlBar() {
        //OPEN BUTTON
        Button openButton = new Button("OPEN IMAGE");
        openButton.getStyleClass().add("open-export-button");
        openButton.setOnAction(event -> {
            fileChooser.setTitle("Open Resource image");
            File selectedFile = fileChooser.showOpenDialog(this.stage);
            if (selectedFile != null) {
                openFile(selectedFile);
            }
        });

        //EXPORT BUTTON
        Button exportButton = new Button("EXPORT");
        exportButton.getStyleClass().add("open-export-button");
        exportButton.setOnAction(event -> {
            fileChooser.setTitle("Export image");
            File selectedFile = fileChooser.showSaveDialog(this.stage);
            if (selectedFile != null) {
                saveFile(selectedFile);
            }
        });

        //DARK BACKGROUND SWITCH BUTTON
        Button switchDarkBackgroundButton = new Button("\uf042");
        switchDarkBackgroundButton.setFont(AWESOME_FONT);
        switchDarkBackgroundButton.setOnAction(event -> {
            switchDarkBackgroundButton.getStyleClass().set(2, imageASCIIJFX.isOnDarkBackground() ? "white-switch-button" : "black-switch-button");
            imageASCIIJFX.setOnDarkBackground(!imageASCIIJFX.isOnDarkBackground());
        });
        switchDarkBackgroundButton.getStyleClass().addAll("switch-button", "white-switch-button");

        //COLOR PICKER
        ColorPicker colorPicker = new ColorPicker();
        colorPicker.valueProperty().bindBidirectional(imageASCIIJFX.backgroundColorProperty());

        //CONTROL BAR
        HBox controlBar = new HBox(openButton,
                createParamField("Page width (px):",        pageWidthProperty),
                createParamField("Page height (px):",       pageHeightProperty),
                createParamField("Render width (char):",    widthProperty),
                createParamField("Render height (char):",   heightProperty),
                createChoiceBox("Scale used:", List.of(ImageASCIIJFX.Scales.values()), ImageASCIIJFX.Scales.STANDARD, imageASCIIJFX.selectedScaleCharProperty()),
                createChoiceBox("Font:", List.of(ImageASCIIJFX.Fonts.values()), ImageASCIIJFX.Fonts.DEJA_VU, imageASCIIJFX.usedFontProperty()),
                new Label("Background:"), colorPicker,
                switchDarkBackgroundButton,
                exportButton);

        controlBar.getStyleClass().add("control-bar");
        return controlBar;
    }

    /**
     * Create a parameter field for a positive integer parameter
     *
     * @param labelText text of the label
     * @param bindProperty property to bind with the textField
     * @return a HBox with a label and a textField bound to a property
     */
    private HBox createParamField(String labelText, IntegerProperty bindProperty) {
        TextField textField = new TextField();
        textField.setTextFormatter(integerFormatter(bindProperty));
        HBox hBox = new HBox(new Label(labelText), textField);
        hBox.getStyleClass().add("HBox");
        return hBox;
    }

    private <T> HBox createChoiceBox(String labelText, List<T> choices, T defaultValue, Property<T> toBind) {
        ChoiceBox<T> choiceBox = new ChoiceBox<>();
        choiceBox.setItems(FXCollections.observableList(choices));
        choiceBox.valueProperty().bindBidirectional(toBind);
        choiceBox.valueProperty().set(defaultValue);
        HBox hBox = new HBox(new Label(labelText), choiceBox);
        hBox.getStyleClass().add("HBox");
        return hBox;
    }

    /**
     *Create an integer formatter for text field allowing only positive integer
     *
     * @param bindProperty property to bind
     * @return an integer formatter for text field allowing only positive integer
     */
    private TextFormatter<Number> integerFormatter(IntegerProperty bindProperty) {
        NumberStringConverter stringConverter = new NumberStringConverter("#0");
        UnaryOperator<TextFormatter.Change> filter = (change -> {
            try {
                String newText = change.getControlNewText();
                int newValue = stringConverter.fromString(newText).intValue();
                return (0 < newValue && newValue < 10_000) ? change : null;
            } catch (Exception e) {
                return null;
            }
        });
        TextFormatter<Number> textFormatter = new TextFormatter<>(stringConverter, 0, filter);
        textFormatter.valueProperty().bindBidirectional(bindProperty);
        return textFormatter;
    }

    /**
     * Create an image pane view to display an image
     * @param propertyToBind image to display in the view
     * @return an image pane view to display an image
     */
    private Pane createImgViewPane(ObjectProperty<Image> propertyToBind) {
        StackPane parent = new StackPane();
        parent.getStyleClass().add("stackPane");
        ImageView imgView = new ImageView();
        imgView.imageProperty().bind(propertyToBind);
        imgView.setPreserveRatio(true);
        imgView.fitWidthProperty().bind(parent.widthProperty());
        imgView.fitHeightProperty().bind(parent.heightProperty());
        parent.getChildren().add(imgView);
        return parent;
    }


    /**
     * Open an image
     * @param file the file with the image to open
     */
    private void openFile(File file) {
        try {
            imageRefProperty.set(SwingFXUtils.toFXImage(ImageIO.read(file), null));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Save an image
     * @param file the file with the image to save
     */
    private void saveFile(File file) {
        try {
            ImageIO.write(SwingFXUtils.fromFXImage(imageASCIIProperty.get(), null), "png", file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Load font used for special characters
     * @return a font used for special characters
     */
    private static Font loadFont() {
        try (InputStream fontStream = Main.class.getResourceAsStream(AWESOME_FONT_PATH)) {
            return Font.loadFont(fontStream, 12);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Font.getDefault();
    }
}