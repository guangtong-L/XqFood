package ai.xiaodudou.module.ai.service;

import ai.xiaodudou.common.exception.BusinessException;
import ai.xiaodudou.common.result.ResultCode;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

@Component
public class ImageUploadValidator {
    static final long MAX_BYTES = 5L * 1024 * 1024;
    static final int MAX_SIDE = 6000;
    static final long MAX_PIXELS = 20_000_000L;
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png");

    public ValidatedImage validate(MultipartFile file) {
        if (file == null || file.isEmpty()) bad(ResultCode.BAD_REQUEST, "图片不能为空");
        if (file.getSize() > MAX_BYTES) bad(ResultCode.PAYLOAD_TOO_LARGE, "图片不能超过 5MB");
        String declaredType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(declaredType)) bad(ResultCode.BAD_REQUEST, "仅支持 JPEG 或 PNG 图片");

        final byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE, "读取上传图片失败");
        }
        String magicType = magicType(bytes);
        if (!declaredType.equals(magicType)) bad(ResultCode.INVALID_IMAGE, "图片声明格式与实际内容不一致");

        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (input == null) bad(ResultCode.INVALID_IMAGE, "无法读取图片内容");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) bad(ResultCode.INVALID_IMAGE, "无法解析图片内容");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                long pixels = Math.multiplyExact((long) width, (long) height);
                if (width <= 0 || height <= 0 || width > MAX_SIDE || height > MAX_SIDE || pixels > MAX_PIXELS) {
                    bad(ResultCode.INVALID_IMAGE, "图片尺寸过大或无效");
                }
                return new ValidatedImage(bytes, magicType, width, height);
            } finally {
                reader.dispose();
            }
        } catch (IOException | ArithmeticException e) {
            throw new BusinessException(ResultCode.INVALID_IMAGE, "无法解析图片内容");
        }
    }

    private String magicType(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff) {
            return "image/jpeg";
        }
        byte[] png = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
        if (bytes.length >= png.length) {
            boolean matched = true;
            for (int i = 0; i < png.length; i++) matched &= bytes[i] == png[i];
            if (matched) return "image/png";
        }
        bad(ResultCode.INVALID_IMAGE, "图片内容不是有效的 JPEG 或 PNG");
        return null;
    }

    private void bad(ResultCode code, String message) {
        throw new BusinessException(code, message);
    }
}
