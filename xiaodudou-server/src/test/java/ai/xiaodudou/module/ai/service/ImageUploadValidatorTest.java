package ai.xiaodudou.module.ai.service;

import ai.xiaodudou.common.exception.BusinessException;
import ai.xiaodudou.common.result.ResultCode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ImageUploadValidatorTest {
    private final ImageUploadValidator validator = new ImageUploadValidator();

    @Test
    void acceptsRealPngAndReportsVerifiedDimensions() throws Exception {
        byte[] bytes = image("png", 16, 12);
        ValidatedImage result = validator.validate(file(bytes, "image/png"));
        assertEquals("image/png", result.mediaType());
        assertEquals(16, result.width());
        assertEquals(12, result.height());
    }

    @Test
    void rejectsEmptyOversizedUnsupportedAndMagicMismatch() {
        assertCode(ResultCode.BAD_REQUEST, () -> validator.validate(file(new byte[0], "image/png")));
        assertCode(ResultCode.PAYLOAD_TOO_LARGE,
                () -> validator.validate(file(new byte[(int) ImageUploadValidator.MAX_BYTES + 1], "image/png")));
        assertCode(ResultCode.BAD_REQUEST, () -> validator.validate(file(new byte[]{1}, "image/gif")));
        assertCode(ResultCode.INVALID_IMAGE, () -> validator.validate(file(new byte[]{1, 2, 3}, "image/png")));
    }

    @Test
    void rejectsDeclaredTypeMismatchAndExcessiveDimensions() throws Exception {
        assertCode(ResultCode.INVALID_IMAGE,
                () -> validator.validate(file(image("png", 2, 2), "image/jpeg")));
        assertCode(ResultCode.INVALID_IMAGE,
                () -> validator.validate(file(image("png", ImageUploadValidator.MAX_SIDE + 1, 1), "image/png")));
    }

    private MockMultipartFile file(byte[] bytes, String type) {
        return new MockMultipartFile("image", "upload", type, bytes);
    }

    private byte[] image(String format, int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, format, out));
        return out.toByteArray();
    }

    private void assertCode(ResultCode expected, ThrowingCall call) {
        BusinessException error = assertThrows(BusinessException.class, call::run);
        assertEquals(expected.getCode(), error.getCode());
    }

    private interface ThrowingCall { void run() throws Exception; }
}
