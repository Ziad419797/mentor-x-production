package com.educore.lessonmaterial;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility لاستخراج YouTube Video ID من أي صيغة URL.
 *
 * يدعم الصيغ دي كلها:
 *   https://www.youtube.com/watch?v=dQw4w9WgXcQ
 *   https://youtu.be/dQw4w9WgXcQ
 *   https://youtube.com/embed/dQw4w9WgXcQ
 *   https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=30s
 *   https://youtu.be/dQw4w9WgXcQ?t=30
 *   https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ
 */
public final class YoutubeUrlUtil {

    private YoutubeUrlUtil() {}

    // Regex يغطي كل صيغ YouTube
    private static final Pattern YOUTUBE_PATTERN = Pattern.compile(
            "(?:youtube\\.com/(?:watch\\?.*v=|embed/|v/|shorts/)|youtu\\.be/)([a-zA-Z0-9_-]{11})"
    );

    /**
     * يستخرج الـ Video ID من URL.
     * @return Video ID (11 حرف) أو null لو الـ URL مش YouTube
     */
    public static String extractVideoId(String url) {
        if (url == null || url.isBlank()) return null;

        Matcher matcher = YOUTUBE_PATTERN.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * هل الـ URL ده YouTube؟
     */
    public static boolean isYoutubeUrl(String url) {
        return extractVideoId(url) != null;
    }

    /**
     * يبني الـ embed URL الجاهز للـ iframe من الـ Video ID.
     *   rel=0          → ما يعرضش فيديوهات تانية بعد التشغيل
     *   modestbranding → يخفي اسم YouTube قدر الإمكان
     *   enablejsapi=1  → يسمح للفرونتند يـ control الـ player
     */
    public static String toEmbedUrl(String videoId) {
        if (videoId == null || videoId.isBlank()) return null;
        return "https://www.youtube.com/embed/" + videoId
                + "?rel=0&modestbranding=1&enablejsapi=1";
    }

    /**
     * Shortcut: من URL مباشرة للـ embed URL.
     * @return embed URL أو null لو مش YouTube
     */
    public static String urlToEmbed(String url) {
        String videoId = extractVideoId(url);
        return videoId != null ? toEmbedUrl(videoId) : null;
    }
}
