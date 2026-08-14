# Layihə qaydaları

## Verilənlər bazası adlandırması

- Tətbiq tərəfindən yaradılan bütün yeni verilənlər bazası cədvəllərinin adı `kn_` prefiksi ilə başlamalıdır.
- Yeni cədvəl yaradılarkən `rn_` prefiksindən istifadə edilməməlidir.
- Bu qayda yalnız yeni cədvəllərə aiddir; mövcud `rn_` cədvəlləri geriyə uyğunluq üçün olduğu kimi saxlanılır.

## Canlı verilənlər bazasına dəyişiklik

- İstifadəçinin həmin dəyişiklik üçün ayrıca və açıq razılığı olmadan canlı DB-də heç bir dəyişiklik tətbiq edilməməlidir.
- Migration faylının hazırlanması onun canlı DB-də işə salınmasına icazə demək deyil.
- DB dəyişiklikləri əvvəlcə yalnız kodda hazırlanmalı, istifadəçiyə göstərilməli və tətbiq etmək üçün ayrıca təsdiq alınmalıdır.
- Test və diaqnostika zamanı canlı DB-yə yazan `INSERT`, `UPDATE`, `DELETE`, DDL və Flyway migrate əməliyyatları təsdiqsiz icra edilməməlidir.
- Flyway standart olaraq söndürülmüş saxlanılmalıdır; istifadəçinin ayrıca razılığı olmadan aktivləşdirilməməli və migrate işlədilməməlidir.
