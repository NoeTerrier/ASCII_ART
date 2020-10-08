import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class ImageASCIIJFX {

    private static final int FONT_SIZE = 15;
    private static final double CHAR_SPACING_X = 10;
    private static final double CHAR_SPACING_Y = 15;

    public enum Fonts {
        SYSTEM_DEFAULT(Font.getDefault()),
        MAJOR_MONO_DISPLAY(loadFont("/MajorMonoDisplay-Regular.ttf")),
        DEJA_VU(new Font("DejaVu Sans Mono", FONT_SIZE)),
        OXYGEN(loadFont("/OxygenMono-Regular.ttf")),
        ROBOTO(loadFont("/RobotoMono-Regular.ttf"));

        private final String name;
        private final Font font;

        Fonts(Font loadFont) {
            this.name = loadFont.getName();
            this.font = loadFont;
        }

        public Font getFont() {
            return font;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum Scales {
        ALPHABET("Alphabet", new Character[]{'W', 'B', 'R', 'H', 'K', 'A', 'S', 'V', 'C', 'y', 'o', 'i', ' '}),
        BINARY("Binary", new Character[]{'0', '1'}),
        BLOCKS("Blocks", new Character[]{'█', '█', '▓', '▒', '░', ' '}),
        STANDARD("Standard", new Character[]{'#', '$', 'o', '{', '+', '~', ':', '-', '.', ' '}),
        STANDARD_2( "Standard 2", new Character[]{'&', '#','{', '$', '#','o', '+', '~',':', '-', '.', ' ', ' '}),
        STANDARD_3("Standard 3", new Character[]{'B', '@', '#','S', '%','?', '*','+', ';', ':',',', '.', ' '});

        private final String name;
        private final Character[] scale;

        Scales(String name, Character[] characters) {
            this.name = name;
            this.scale = characters;
        }

        public Character[] getScale() {
            return scale;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    //MARK:- CONSTANTS
    private final static int IMAGE_TYPE = BufferedImage.TYPE_BYTE_GRAY;
    private final static double MIN_RGB_VALUE = -Math.pow(2, 24), MAX_RGB_VALUE = 0;

    //MARK:- ATTRIBUTES
    private final IntegerProperty height = new SimpleIntegerProperty(100);
    private final IntegerProperty width = new SimpleIntegerProperty(100);
    private final IntegerProperty pageHeight = new SimpleIntegerProperty(2000);
    private final IntegerProperty pageWidth = new SimpleIntegerProperty(2000);
    private final BooleanProperty onDarkBackground = new SimpleBooleanProperty(false);


    private final ObjectProperty<BufferedImage> imageRef = new SimpleObjectProperty<>(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB));
    private final ObjectProperty<Scales> selectedScaleChar = new SimpleObjectProperty<>(Scales.STANDARD);
    private final ObjectProperty<Fonts> usedFont = new SimpleObjectProperty<>(Fonts.DEJA_VU);
    private final ObjectProperty<Color> backgroundColor = new SimpleObjectProperty<>(Color.WHITE);
    private final ObjectBinding<BufferedImage> imageASCII;
    private final ObjectBinding<Integer[][]> RGBValues;
    private final ObjectBinding<Character[][]> charValues;
    private final ObjectBinding<Character[]> usedScaleChar;

    private final Canvas canvas = new Canvas();
    private final GraphicsContext ctx = canvas.getGraphicsContext2D();

    /**
     * Constructor of ImageASCII from a buffered Image
     */
    public ImageASCIIJFX() {
        ctx.setTextBaseline(VPos.CENTER);
        ctx.setTextAlign(TextAlignment.CENTER);

        usedScaleChar = Bindings.createObjectBinding(() -> isOnDarkBackground() ? reverse(getSelectedScaleChar().getScale()) : getSelectedScaleChar().getScale(), backgroundColor, onDarkBackground, selectedScaleChar);

        RGBValues  = Bindings.createObjectBinding(() -> getRGBValues(new GrayScaleImage(resize(imageRef.get()))), height, width, pageHeight, pageWidth, usedScaleChar, usedFont, imageRef);
        charValues = Bindings.createObjectBinding(() -> getCharArray(RGBValues.get()), RGBValues);
        imageASCII = Bindings.createObjectBinding(this::getRepresentation, charValues, imageRef);
    }

    /**
     * Return an 2D array of the RGB values of the image's pixel matrix
     *
     * @param image the reference image
     * @return an 2D array of the RGB values of the image's pixel matrix
     */
    private Integer[][] getRGBValues(BufferedImage image) {
        Integer[][] result = new Integer[image.getHeight()][image.getWidth()];

        for (int r = 0; r < image.getHeight(); ++r) {
            for (int c = 0; c < image.getWidth(); ++c) {
                result[r][c] = image.getRGB(c, r);
            }
        }
        return result;
    }

    /**
     * Return a representation of a RGB values table in ASCII
     *
     * @param RGBArray the array to represent
     * @return the representation of the array with associated RGB values
     */
    private Character[][] getCharArray(Integer[][] RGBArray) {
        int height = RGBArray.length;
        int width = height != 0 ? RGBArray[0].length : 0;

        Character[][] result = new Character[RGBArray.length][width];

        for (int r = 0; r < height; ++r) {
            for (int c = 0; c < width; ++c) {
                int index = (int) Math.round(map(RGBArray[r][c], MIN_RGB_VALUE, MAX_RGB_VALUE, 0, usedScaleChar.get().length - 1));
                result[r][c] = usedScaleChar.get()[index];
            }
        }
        return result;
    }

    /**
     * Resize an image to the height/width dimension of the ImageASCII
     *
     * @param image the reference image
     * @return the resized image
     */
    private BufferedImage resize(BufferedImage image) {
        Image resizedImage = image.getScaledInstance(getWidth(), getHeight(), Image.SCALE_SMOOTH);
        BufferedImage bufferedImage = new BufferedImage(getWidth(), getHeight(), IMAGE_TYPE);
        Graphics2D graphics = bufferedImage.createGraphics();
        graphics.drawImage(resizedImage, 0, 0, null);
        graphics.dispose();

        return bufferedImage;
    }

    /**
     * Return an image of the ImageASCII
     *
     * @return an image of the ImageASCII
     */
    private BufferedImage getRepresentation() {
        ctx.setFont(getUsedFont().getFont());
        ctx.clearRect(0, 0, getPageWidth(), getPageHeight());

        getHeight();
        getWidth();

        if (getImageRef() != null) {
            canvas.setHeight(getPageHeight());
            canvas.setWidth(getPageWidth());
            ctx.setFill(getBackgroundColor());
            ctx.fillRect(0, 0, getPageWidth(), getPageHeight());
            ctx.setFill(isOnDarkBackground() ? Color.WHITE : Color.BLACK);

            double deltaY = (getPageHeight() - charValues.get().length * CHAR_SPACING_Y) / 2;
            double deltaX = (getPageWidth()  - charValues.get()[0].length * CHAR_SPACING_X) / 2;

            ctx.fillText("Scale used : " + Arrays.toString(getUsedScaleChar()), getPageWidth()/2.0, deltaY - CHAR_SPACING_Y);

            for (int i = 0; i < charValues.get().length; ++i) {
                for (int j = 0; j < charValues.get()[i].length; ++j) {
                    ctx.setFill(Color.grayRgb(RGBValues.get()[i][j] & 255));
                    ctx.fillText(String.valueOf(charValues.get()[i][j]), j * CHAR_SPACING_X + deltaX, i * CHAR_SPACING_Y + deltaY);
                }
            }

            return SwingFXUtils.fromFXImage(canvas.snapshot(null, null), null);
        }
        return null;
    }

    /**
     * Return String representation of the ImageASCII
     *
     * @return String representation of the ImageASCII
     */
    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder("Scale used: ").append(Arrays.toString(getUsedScaleChar())).append("\n");
        for (Character[] chars : charValues.get()) {
            for (char c : chars) {
                stringBuilder.append(c);
            }
            stringBuilder.append('\n');
        }
        return stringBuilder.toString();
    }

    public int getHeight() {
        return height.get();
    }

    public IntegerProperty heightProperty() {
        return height;
    }

    public void setHeight(int height) {
        this.height.set(height);
    }

    public int getWidth() {
        return width.get();
    }

    public IntegerProperty widthProperty() {
        return width;
    }

    public void setWidth(int width) {
        this.width.set(width);
    }

    public int getPageHeight() {
        return pageHeight.get();
    }

    public IntegerProperty pageHeightProperty() {
        return pageHeight;
    }

    public void setPageHeight(int pageHeight) {
        this.pageHeight.set(pageHeight);
    }

    public int getPageWidth() {
        return pageWidth.get();
    }

    public IntegerProperty pageWidthProperty() {
        return pageWidth;
    }

    public void setPageWidth(int pageWidth) {
        this.pageWidth.set(pageWidth);
    }

    public boolean isOnDarkBackground() {
        return onDarkBackground.get();
    }

    public BooleanProperty onDarkBackgroundProperty() {
        return onDarkBackground;
    }

    public void setOnDarkBackground(boolean onDarkBackground) {
        this.onDarkBackground.set(onDarkBackground);
    }

    public BufferedImage getImageRef() {
        return imageRef.get();
    }

    public ObjectProperty<BufferedImage> imageRefProperty() {
        return imageRef;
    }

    public void setImageRef(BufferedImage imageRef) {
        this.imageRef.set(imageRef);
    }

    public BufferedImage getImageASCII() {
        return imageASCII.get();
    }

    public ObjectBinding<BufferedImage> imageASCIIProperty() {
        return imageASCII;
    }

    public Character[] getUsedScaleChar() {
        return usedScaleChar.get();
    }

    public ObjectBinding<Character[]> usedScaleCharProperty() {
        return usedScaleChar;
    }

    public Scales getSelectedScaleChar() {
        return selectedScaleChar.get();
    }

    public ObjectProperty<Scales> selectedScaleCharProperty() {
        return selectedScaleChar;
    }

    public void setSelectedScaleChar(Scales selectedScaleChar) {
        this.selectedScaleChar.set(selectedScaleChar);
    }

    public Fonts getUsedFont() {
        return usedFont.get();
    }

    public ObjectProperty<Fonts> usedFontProperty() {
        return usedFont;
    }

    public void setUsedFont(Fonts usedFont) {
        this.usedFont.set(usedFont);
    }

    public Color getBackgroundColor() {
        return backgroundColor.get();
    }

    public ObjectProperty<Color> backgroundColorProperty() {
        return backgroundColor;
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor.set(backgroundColor);
    }

//MARK:- TOOLS

    /**
     * Map a number from a scale to another scale
     *
     * @param number      the number to scale
     * @param firstLow    the lower bound of the first interval
     * @param firstUpper  the upper bound of the first interval
     * @param secondLow   the lower bound of the second interval
     * @param secondUpper the upper bound of the second interval
     * @return the mapped number
     */
    private double map(double number, double firstLow, double firstUpper, double secondLow, double secondUpper) {
        if ((firstLow > firstUpper) || (secondLow > secondUpper) || !(firstLow <= number && number <= firstUpper)) {
            throw new IllegalArgumentException();
        }
        double ratio = (number - firstLow) / (firstUpper - firstLow);
        return ratio * (secondUpper - secondLow) + secondLow;
    }

    /**
     * Reverse an array
     *
     * @param array the array to reverse
     * @return the reversed array
     */
    private Character[] reverse(Character[] array) {
        Character[] tmp = new Character[array.length];
        for (int i = 0; i < array.length; ++i) {
            tmp[i] = array[array.length - i - 1];
        }
        return tmp;
    }

    /**
     * Load font used for special characters
     * @return a font used for special characters
     */
    private static Font loadFont(String path) {
        try (InputStream fontStream = Main.class.getResourceAsStream(path)) {
            return Font.loadFont(fontStream, FONT_SIZE);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Font.getDefault();
    }
}
