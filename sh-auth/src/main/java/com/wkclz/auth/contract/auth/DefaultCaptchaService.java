package com.wkclz.auth.contract.auth;

import com.wkclz.auth.bean.Captcha;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;

/**
 * 基于 Java AWT Graphics2D 的图形验证码默认实现
 */
public class DefaultCaptchaService implements CaptchaService {

    private static final long CAPTCHA_TTL_MINUTES = 5;
    private static final int CAPTCHA_LENGTH = 4;
    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;
    private static final int FONT_SIZE = 20;
    private static final String CHARACTERS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";

    private final Cache<String, String> captchaCache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(CAPTCHA_TTL_MINUTES))
            .build();

    @Override
    public Captcha generate() {
        String captchaCode = generateCaptchaCode();
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        String base64Image = generateCaptchaImage(captchaCode);

        captchaCache.put(captchaId, captchaCode);

        Captcha captcha = new Captcha();
        captcha.setCaptchaId(captchaId);
        captcha.setCaptchaCode(captchaCode);
        captcha.setCaptchaImage(base64Image);
        captcha.setExpireTime(LocalDateTime.now().plusMinutes(CAPTCHA_TTL_MINUTES));
        return captcha;
    }

    @Override
    public boolean verify(String captchaId, String captchaCode) {
        if (captchaId == null || captchaCode == null) {
            return false;
        }
        String stored = captchaCache.getIfPresent(captchaId);
        if (stored == null) {
            return false;
        }
        boolean matched = stored.equalsIgnoreCase(captchaCode);
        captchaCache.invalidate(captchaId);
        return matched;
    }

    // ===== 验证码生成 =====

    private String generateCaptchaCode() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CAPTCHA_LENGTH; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    private String generateCaptchaImage(String captchaCode) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        Font font = new Font("Arial", Font.BOLD, FONT_SIZE);
        g.setFont(font);

        drawInterferenceLines(g);
        drawNoisePoints(g);
        drawCharacters(g, captchaCode);

        g.dispose();
        return "data:image/png;base64," + imageToBase64(image);
    }

    private void drawInterferenceLines(Graphics2D g) {
        Random random = new Random();
        g.setColor(Color.LIGHT_GRAY);
        for (int i = 0; i < 5; i++) {
            int x1 = random.nextInt(WIDTH);
            int y1 = random.nextInt(HEIGHT);
            int x2 = random.nextInt(WIDTH);
            int y2 = random.nextInt(HEIGHT);
            g.drawLine(x1, y1, x2, y2);
        }
    }

    private void drawNoisePoints(Graphics2D g) {
        Random random = new Random();
        for (int i = 0; i < 50; i++) {
            int x = random.nextInt(WIDTH);
            int y = random.nextInt(HEIGHT);
            int radius = random.nextInt(6);
            g.setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
            g.fillOval(x, y, radius, radius);
        }
    }

    private void drawCharacters(Graphics2D g, String captchaCode) {
        Random random = new Random();
        int charWidth = WIDTH / captchaCode.length();
        for (int i = 0; i < captchaCode.length(); i++) {
            g.setColor(new Color(random.nextInt(100), random.nextInt(100), random.nextInt(100)));
            double angle = random.nextDouble() * 0.4 - 0.2;
            g.rotate(angle, i * charWidth + (double) charWidth / 2, (double) HEIGHT / 2);
            g.drawString(String.valueOf(captchaCode.charAt(i)), i * charWidth + 10, HEIGHT / 2 + 8);
            g.rotate(-angle, i * charWidth + (double) charWidth / 2, (double) HEIGHT / 2);
        }
    }

    private String imageToBase64(BufferedImage image) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("验证码图片生成失败", e);
        }
    }
}
