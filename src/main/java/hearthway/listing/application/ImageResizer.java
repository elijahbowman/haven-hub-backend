package hearthway.listing.application;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

/**
 * Scales raster images down to a maximum width as JPEG, using only the JDK.
 */
final class ImageResizer {

    private static final float JPEG_QUALITY = 0.82f;

    private ImageResizer() {
    }

    /**
     * Returns the image scaled to {@code maxWidth} as JPEG, or empty when the bytes
     * aren't a format ImageIO can read (e.g. SVG or WebP) or the image is already
     * that narrow, in which case the caller should serve the original.
     */
    static Optional<byte[]> scaleToJpeg(byte[] original, int maxWidth) {
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(original));
            if (source == null || source.getWidth() <= maxWidth) {
                return Optional.empty();
            }

            int height = Math.max(1, Math.round(source.getHeight() * (maxWidth / (float) source.getWidth())));
            BufferedImage scaled = new BufferedImage(maxWidth, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = scaled.createGraphics();
            try {
                // JPEG has no transparency: paint transparent areas white.
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, maxWidth, height);
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                graphics.drawImage(source, 0, 0, maxWidth, height, null);
            } finally {
                graphics.dispose();
            }
            return Optional.of(writeJpeg(scaled));
        } catch (IOException | RuntimeException unreadable) {
            return Optional.empty();
        }
    }

    private static byte[] writeJpeg(BufferedImage image) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ImageOutputStream output = ImageIO.createImageOutputStream(bytes)) {
            writer.setOutput(output);
            ImageWriteParam params = writer.getDefaultWriteParam();
            params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            params.setCompressionQuality(JPEG_QUALITY);
            writer.write(null, new IIOImage(image, null, null), params);
        } finally {
            writer.dispose();
        }
        return bytes.toByteArray();
    }
}
