import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * @author noe
 * <p>
 * BufferedImage representing a gray scaled image from another image
 */
public class GrayScaleImage extends BufferedImage {

    /**
     * Constructor of GrayScaleImage
     *
     * @param image the reference image
     */
    public GrayScaleImage(BufferedImage image) {
        super(image.getWidth(), image.getHeight(), image.getType());
        setData(grayScaleImage(image).getData());
    }

    /**
     * Return an image copied in gray scale
     *
     * @param image the image to scale
     * @return the gray scaled image
     */
    private BufferedImage grayScaleImage(BufferedImage image) {
        double red, green, blue;
        int gray;
        Color color;

        BufferedImage copy = new BufferedImage(
                image.getColorModel(),
                image.copyData(null),
                image.getColorModel().isAlphaPremultiplied(),
                null);

        for (int r = 0; r < image.getHeight(); ++r) {
            for (int c = 0; c < image.getWidth(); ++c) {
                color = new Color(image.getRGB(c, r));

                red   = color.getRed() * 0.299;
                green = color.getGreen() * 0.587;
                blue  = color.getBlue() * 0.1114;
                gray  = (int) (red + green + blue);

                copy.setRGB(c, r, new Color(gray, gray, gray).getRGB());
            }
        }
        return copy;
    }
}
