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

## Testlər və build yoxlaması

- İstifadəçi xüsusi olaraq istəmədiyi halda test class-ları, unit testlər, integration testlər, `@SpringBootTest` və MockMvc testləri yaradılmamalıdır.
- Silinmiş testlər və yalnız testlər üçün olan fayl, dependency və konfiqurasiyalar istifadəçinin xüsusi istəyi olmadan yenidən əlavə edilməməlidir.
- Test təmizliyi zamanı production koduna və proqramın mövcud işləmə məntiqinə toxunulmamalıdır.
- Java kodunu yoxlamaq lazım olduqda mümkün olan ən minimal Maven əmri istifadə edilməlidir. Tam paket build-i tələb olunarsa `./mvnw clean package -DskipTests` istifadə edilməli və uğurla tamamlandığı yoxlanılmalıdır.

## VACİB — Minimal iş qaydası

- Yalnız istifadəçinin açıq şəkildə istədiyi işi gör; istənilməyən əlavə dəyişiklik və təkmilləşdirmə etmə.
- Tapşırıqla əlaqəsi olmayan kodu refaktor etmə və modulları analiz etmə.
- Tapşırıq konkret fayl və ya class-larla bağlıdırsa, bütün layihəni lazımsız yerə analiz etmə.
- İstifadəçi xüsusi olaraq istəmədiyi halda test yaratma və mövcud testləri dəyişmə. Heç vaxt avtomatik test yaratma.
- Bütün testləri (full test suite) özbaşına işə salma.
- “Onsuz da buradayam, bunu da düzəldim” prinsipi ilə əlavə iş görmə.
- Tapşırıq üçün zəruri deyilsə əlavə validation, abstraction, helper class, documentation və cleanup yaratma.
- Tapşırığı düzgün yerinə yetirən mümkün qədər kiçik və konkret kod dəyişikliyinə üstünlük ver; minimum sayda fayla toxun.
- Dəyişiklikdən sonra yalnız həmin dəyişiklik üçün zəruri olan yoxlamanı apar.
- Hər kiçik dəyişiklikdən sonra təkrar-təkrar build, test və analiz etmə.
- Java kodunu yoxlamaq lazım olduqda mümkün olan ən minimal Maven əmri istifadə et.
- Lazımsız yerə çoxlu terminal əmrləri işlətmə.
- Tapşırığı düzgün yerinə yetirmək üçün istənilən iş çərçivəsindən kənara çıxmaq mütləq lazımdırsa, əlavə işi özbaşına görmə; əvvəlcə istifadəçiyə bildir və icazə al.

Əsas prinsip: İstənilən işi et → minimum sayda fayla toxun → minimum zəruri yoxlama apar → işi bitir və dayan.

## Mövcud kod stilini qoru

- Layihədə artıq istifadə olunan kod yazılış formasını davam etdir.
- Mövcud sadə həlli özbaşına mürəkkəbləşdirmə.
- İstifadəçi istəmədiyi halda helper metodlar yaratma.
- İstifadəçi istəmədiyi halda əlavə mapper, wrapper, utility, abstraction və service layer yaratma.
- JDBC ResultSet mapping zamanı layihədə mövcud olan sadə mapping formasını istifadə et.
- Hər column üçün ayrıca helper və ya əlavə metod yaratma.
- Eyni işi mövcud kod stili ilə birbaşa etmək mümkündürsə, yeni struktur yaratma.
- “Clean code”, “best practice” və ya gələcək ehtimallar adı ilə lazımsız kod artırma.
- Mövcud işləyən yanaşmanı yalnız real texniki zərurət olduqda dəyiş. Belə zərurət yaranarsa, özbaşına dəyişmə; əvvəl istifadəçiyə bildir.

Əsas qayda: Layihədə həmin iş necə görülürsə, eyni formada davam et. Sadə işi mürəkkəbləşdirmə. Minimum kod yaz.
