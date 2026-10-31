package hearthway.listing.presentation;

import hearthway.listing.application.PictureService;
import hearthway.listing.application.PictureService.PictureContent;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

/**
 * Serves listing pictures by URL so list and detail responses don't embed image
 * bytes. Pictures never change (a new picture gets a new id), so responses are
 * cacheable for a year.
 */
@RestController
@RequestMapping("/api/pictures")
public class PictureResource {

    // The stored content type comes from the uploader. Only these are served
    // inline; anything else (e.g. text/html) is sent as a download so it can't run
    // on this origin.
    private static final Set<String> INLINE_IMAGE_TYPES =
            Set.of("image/jpeg", "image/png", "image/gif", "image/webp", "image/svg+xml");

    private final PictureService pictureService;

    public PictureResource(PictureService pictureService) {
        this.pictureService = pictureService;
    }

    @GetMapping("/{publicId}")
    public ResponseEntity<byte[]> get(@PathVariable UUID publicId,
                                      @RequestParam(required = false) Integer width) {
        return pictureService.getContent(publicId, width)
                .map(PictureResource::toResponse)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleUnsupportedWidth(IllegalArgumentException exception) {
        return ResponseEntity.badRequest()
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage()));
    }

    private static ResponseEntity<byte[]> toResponse(PictureContent content) {
        ResponseEntity.BodyBuilder response = ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .header("X-Content-Type-Options", "nosniff")
                // If a picture is opened directly, nothing in it (e.g. SVG scripts) can run.
                .header("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'; sandbox");

        String contentType = content.contentType() == null ? "" : content.contentType().toLowerCase();
        if (INLINE_IMAGE_TYPES.contains(contentType)) {
            return response.contentType(MediaType.parseMediaType(contentType)).body(content.bytes());
        }
        return response.contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().build().toString())
                .body(content.bytes());
    }
}
