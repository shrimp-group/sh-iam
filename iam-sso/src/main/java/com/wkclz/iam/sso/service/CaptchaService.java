package com.wkclz.iam.sso.service;

import com.wkclz.core.exception.SystemException;
import com.wkclz.iam.sso.bean.resp.PictureCaptchaResp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 验证码服务 — 图形验证码生成 + Redis 原子校验。
 *
 * <p>验证码存入 Redis，Key 为 {@code iam:captcha:{captchaId}}，TTL 5min。
 * 校验使用 Redis 的 get-and-delete 实现一次性消费。</p>
 */
@Service
public class CaptchaService {

    private static final Logger log = LoggerFactory.getLogger(CaptchaService.class);

    private static final long CAPTCHA_TTL_MINUTES = 5;
    private static final String REDIS_KEY_PREFIX = "iam:captcha:";
    private static final int CAPTCHA_LENGTH = 4;
    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;
    private static final int FONT_SIZE = 20;
    private static final String CHARACTERS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";

    @Autowired
    private StringRedisTemplate redisTemplate;

    public PictureCaptchaResp generate() {
        String captchaCode = generateCaptchaCode();
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        String base64Image = generateCaptchaImage(captchaCode);

        // 存入 Redis，TTL 5min
        String redisKey = REDIS_KEY_PREFIX + captchaId;
        redisTemplate.opsForValue().set(redisKey, captchaCode, CAPTCHA_TTL_MINUTES, TimeUnit.MINUTES);
        log.debug("Captcha generated: captchaId={}", captchaId);

        return new PictureCaptchaResp(captchaId, base64Image);
    }

    public boolean verify(String captchaId, String captchaCode) {
        if (captchaId == null || captchaCode == null) {
            return false;
        }
        String redisKey = REDIS_KEY_PREFIX + captchaId;
        // 原子 get-and-delete，实现一次性消费
        String storedCode = redisTemplate.opsForValue().getAndDelete(redisKey);
        if (storedCode == null) {
            log.debug("Captcha not found or already consumed: captchaId={}", captchaId);
            return false;
        }
        boolean matched = storedCode.equalsIgnoreCase(captchaCode);
        if (!matched) {
            log.debug("Captcha mismatch: captchaId={}, expected={}, actual={}", captchaId, storedCode, captchaCode);
        }
        return matched;
    }

    // ========== 图形绘制 ==========

    private static String generateCaptchaCode() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CAPTCHA_LENGTH; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    private static String generateCaptchaImage(String captchaCode) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        Font font = new Font("Arial", Font.BOLD, FONT_SIZE);
        g.setFont(font);
        drawInterferenceLines(g);
        drawNoisePoints(g);
        drawCaptchaCharacters(g, captchaCode);
        g.dispose();
        return "data:image/png;base64," + imageToBase64(image);
    }

    private static void drawInterferenceLines(Graphics2D g) {
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

    private static void drawNoisePoints(Graphics2D g) {
        Random random = new Random();
        for (int i = 0; i < 50; i++) {
            int x = random.nextInt(WIDTH);
            int y = random.nextInt(HEIGHT);
            int radius = random.nextInt(6);
            g.setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
            g.fillOval(x, y, radius, radius);
        }
    }

    private static void drawCaptchaCharacters(Graphics2D g, String captchaCode) {
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

    private static String imageToBase64(BufferedImage image) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            throw SystemException.of("验证码图片生成失败");
        }
    }

}
