#!/usr/bin/env ruby
ROOT=File.expand_path("..",__dir__)
pages=File.join(ROOT,"src/main/resources/templates/pages")
replacements={
  "xetaJurnali.html"=>{
    'th:text="|${totalCount} xəta qeydi|"'=>'th:text="#{error_log.count(${totalCount})}"',
    'th:text="|Səhifə ${currentPage} / ${totalPages}|"'=>'th:text="#{error_log.page(${currentPage},${totalPages})}"',
    'th:href="@{/xetaJurnali(q=${q},nov=${nov},baslama=${baslama},bitme=${bitme},page=${currentPage+1})}">Növbəti</a>'=>'th:href="@{/xetaJurnali(q=${q},nov=${nov},baslama=${baslama},bitme=${bitme},page=${currentPage+1})}" th:text="#{error_log.novbeti}">Növbəti</a>',
    'th:text="${x.xetaNovu==\'DB_XETASI\'}?\'DB xətası\':\'Sistem xətası\'"'=>'th:text="${x.xetaNovu==\'DB_XETASI\'} ? #{error_log.db_xetasi} : #{error_log.sistem_xetasi}"'
  },
  "teskilatlar.html"=>{
    'th:text="|${#lists.size(teskilatlar)} təşkilat|"'=>'th:text="#{organizations.count(${#lists.size(teskilatlar)})}"',
    'th:text="${t.aktiv}?\'Aktiv\':\'Passiv\'"'=>'th:text="${t.aktiv} ? #{common.aktiv} : #{common.passiv}"'
  },
  "xidmet.html"=>{
    'th:text="${x.aktiv}?\'Aktiv\':\'Passiv\'"'=>'th:text="${x.aktiv} ? #{common.aktiv} : #{common.passiv}"',
    'th:text="${filterApplied}?\'Filterə uyğun xidmət tapılmadı.\':(${selectedQrupId != null}?\'Bu qrupa aid xidmət tapılmadı.\':\'Xidmət tapılmadı.\')"'=>'th:text="${filterApplied} ? #{services.filter_no_result} : (${selectedQrupId != null} ? #{services.group_no_result} : #{services.no_result})"'
  },
  "shobe.html"=>{
    'th:text="${sobe.aktiv} ? \'Aktiv\' : \'Passiv\'"'=>'th:text="${sobe.aktiv} ? #{common.aktiv} : #{common.passiv}"'
  }
}
replacements.each do |name,map|
  path=File.join(pages,name);source=File.read(path,encoding:"UTF-8");changed=source.dup
  map.each{|old,new_value|raise "Tapılmadı: #{name}: #{old}" unless changed.include?(old);changed.gsub!(old,new_value)}
  File.write(path,changed,encoding:"UTF-8")
end
puts "Dinamik say, status və nəticə mətnləri i18n parametrinə keçirildi."
