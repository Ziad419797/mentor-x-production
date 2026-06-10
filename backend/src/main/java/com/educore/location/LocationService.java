package com.educore.location;


import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class LocationService {
    private Map<String, List<String>> locations;
    // فهرس مطبّع لأسماء المحافظات عشان نقدر نلاقي "اسيوط" حتى لو المستخدم/الطالب مكتوب عنده "أسيوط"
    // (نفس مشكلة فلتر المحافظة اللي كانت في لوحة الطلاب — اختلاف الهمزات/التشكيل)
    private Map<String, String> normalizedToOriginal;

    private static final Pattern DIACRITICS = Pattern.compile("[\\u064B-\\u0652]");

    private static String normalizeArabic(String value) {
        if (value == null) return "";
        String v = value.trim();
        v = DIACRITICS.matcher(v).replaceAll("");
        v = v.replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا');
        v = v.replace('ى', 'ي');
        v = v.replace('ة', 'ه');
        v = v.replaceAll("\\s+", " ");
        return v.toLowerCase();
    }

    public LocationService() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = new ClassPathResource("locations.json").getInputStream();
            locations = mapper.readValue(is, new TypeReference<Map<String, List<String>>>() {});
            normalizedToOriginal = new HashMap<>();
            for (String key : locations.keySet()) {
                normalizedToOriginal.put(normalizeArabic(key), key);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Cacheable("governorates")
    public List<String> getGovernorates() {
        return locations.keySet().stream().toList();
    }
    @Cacheable(value = "areas", key = "#governorate")
    public List<String> getAreas(String governorate) {
        if (locations.containsKey(governorate)) {
            return locations.get(governorate);
        }
        // مفيش تطابق حرفي — نجرب نلاقي نفس المحافظة بعد تطبيع الهمزات/التشكيل
        // (مثال: المستخدم بعت "أسيوط" بينما المفتاح المخزّن "اسيوط")
        String original = normalizedToOriginal.get(normalizeArabic(governorate));
        if (original != null) {
            return locations.getOrDefault(original, List.of());
        }
        return List.of();
    }
}
