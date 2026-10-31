package hearthway.listing;

import hearthway.listing.domain.Listing;
import hearthway.listing.domain.ListingPicture;
import hearthway.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PictureIntegrationTest extends AbstractIntegrationTest {

    private Listing listing;

    @BeforeEach
    void setUp() {
        listing = createListing(createUser("landlord@test.dev"), 2);
    }

    @Test
    void servesTheOriginalWithLongLivedCachingAndWithoutLogin() throws Exception {
        byte[] png = pngOfWidth(800);
        ListingPicture picture = createPicture(listing, png, "image/png", false);

        mockMvc.perform(get("/api/pictures/{id}", picture.getPublicId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(png))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("max-age=31536000")))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("immutable")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    void scalesDownToTheRequestedWidthAsJpeg() throws Exception {
        ListingPicture picture = createPicture(listing, pngOfWidth(2000), "image/png", false);

        byte[] body = mockMvc.perform(get("/api/pictures/{id}", picture.getPublicId()).param("width", "640"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andReturn().getResponse().getContentAsByteArray();

        BufferedImage thumbnail = ImageIO.read(new ByteArrayInputStream(body));
        assertThat(thumbnail.getWidth()).isEqualTo(640);
        assertThat(thumbnail.getHeight()).isEqualTo(320);
    }

    @Test
    void servesTheOriginalWhenItIsAlreadyNarrowerThanRequested() throws Exception {
        byte[] png = pngOfWidth(300);
        ListingPicture picture = createPicture(listing, png, "image/png", false);

        mockMvc.perform(get("/api/pictures/{id}", picture.getPublicId()).param("width", "640"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(png));
    }

    @Test
    void rejectsWidthsOutsideTheAllowedSet() throws Exception {
        ListingPicture picture = createPicture(listing, pngOfWidth(800), "image/png", false);

        mockMvc.perform(get("/api/pictures/{id}", picture.getPublicId()).param("width", "641"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void servesNonImageContentAsADownload() throws Exception {
        // An uploader could claim any content type; HTML must never render on this origin.
        ListingPicture picture = createPicture(listing, "<script>alert(1)</script>".getBytes(), "text/html", false);

        mockMvc.perform(get("/api/pictures/{id}", picture.getPublicId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/octet-stream"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")));
    }

    @Test
    void returnsNotFoundForAnUnknownPicture() throws Exception {
        mockMvc.perform(get("/api/pictures/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    /** A real PNG with a 2:1 aspect ratio. */
    private static byte[] pngOfWidth(int width) throws IOException {
        BufferedImage image = new BufferedImage(width, width / 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bytes);
        return bytes.toByteArray();
    }
}
