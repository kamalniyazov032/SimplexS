# Layihə qaydaları

## Verilənlər bazası adlandırması

- Tətbiq tərəfindən yaradılan bütün yeni verilənlər bazası cədvəllərinin adı `kn_` prefiksi ilə başlamalıdır.
- Yeni cədvəl yaradılarkən `rn_` prefiksindən istifadə edilməməlidir.
- Bu qayda yalnız yeni cədvəllərə aiddir; mövcud `rn_` cədvəlləri geriyə uyğunluq üçün olduğu kimi saxlanılır.

## Canlı verilənlər bazasına dəyişiklik

- Agent cədvəl prefiksindən asılı olmayaraq istifadəçidən həmin konkret əməliyyat üçün ayrıca və açıq razılıq almadan canlı DB-də heç bir dəyişiklik edə bilməz.
- Bu məhdudiyyət `INSERT`, `UPDATE`, `DELETE`, DDL, funksiya/prosedur dəyişiklikləri və Flyway migrate daxil olmaqla bütün yazma əməliyyatlarına aiddir.
- Diaqnostika üçün məlumatı dəyişməyən `SELECT` sorğuları işlədilə bilər; agent bunun yalnız oxuma əməliyyatı olduğunu istifadəçiyə bildirməlidir.
- `rn_` prefiksli mövcud və ya yeni cədvəllərə, onların məlumatlarına və əlaqəli DB obyektlərinə hər hansı dəyişiklik yalnız istifadəçinin həmin dəyişiklik üçün ayrıca və açıq razılığı ilə tətbiq edilə bilər.
- Bir migration həm `kn_`, həm də `rn_` obyektlərinə toxunursa, canlı DB-yə tətbiqdən əvvəl ayrıca razılıq alınmalıdır.
- Migration faylının hazırlanması `rn_` dəyişikliklərini canlı DB-yə tətbiq etməyə icazə sayılmır.
- Flyway standart olaraq söndürülmüş saxlanılmalıdır; yalnız bütün pending migration-lar yuxarıdakı icazə qaydalarına uyğundursa migrate işlədilə bilər.

## Çoxdillilik

- İstifadəçiyə görünən yeni statik mətnlər birbaşa HTML, Java və ya JavaScript daxilində yazılmamalıdır; tərcümə açarı vasitəsilə göstərilməlidir.
- Azərbaycan dili bütün interfeys mətnləri üçün standart və ehtiyat dildir.
- Thymeleaf şablonlarında mətn, başlıq, `placeholder`, `title`, `aria-label` və oxşar atributlar `#{...}` mesaj açarlarından alınmalıdır.
- Java bildirişləri, validasiya və xəta mesajları `MessageSource` vasitəsilə lokallaşdırılmalıdır.
- JavaScript-in istifadəçiyə göstərdiyi mətnlər server tərəfindən verilən lokallaşdırılmış dəyərlərdən və ya ayrıca i18n obyektindən alınmalıdır.
- Yeni interfeys açarı əlavə edilərkən onun Azərbaycan dilində standart dəyəri `messages_az.properties` faylına daxil edilməlidir.
- DB-dən gələn biznes məlumatlarının tərcüməsi interfeys tərcümələrindən ayrı saxlanılmalı və ikinci mərhələnin tərcümə mexanizmi ilə idarə edilməlidir.
