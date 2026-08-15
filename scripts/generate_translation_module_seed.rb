#!/usr/bin/env ruby
require "set"

ROOT=File.expand_path("..",__dir__)
RES=File.join(ROOT,"src/main/resources")
TEMPLATES=File.join(RES,"templates")
PAGE_MODULE={
  "emekdash.html"=>"MIS_PERSONAL", "rollar.html"=>"APP_ROLES", "modullar.html"=>"APP_MODULES",
  "xidmetQiymetleri.html"=>"APP_PRICES", "yataqlar.html"=>"APP_BEDS", "diaqnozlar.html"=>"APP_DIAGNOSES",
  "xidmetQruplari.html"=>"APP_SERVICE_GROUPS", "vezifeler.html"=>"APP_POSITIONS", "kassalar.html"=>"APP_CASH_REGISTERS",
  "binalar.html"=>"APP_BUILDINGS", "muhasibatKodu.html"=>"APP_ACCOUNT_CODES", "shobeXidmet.html"=>"APP_DEPARTMENT_SERVICES",
  "binaParametrleri.html"=>"APP_BUILDING_PARAMS", "tercumeler.html"=>"APP_TRANSLATIONS", "login.html"=>"GLOBAL",
  "shobe.html"=>"MIS_SERVICE", "xidmet.html"=>"MIS_EXAM", "teskilatlar.html"=>"MIS_ORGANIZATION", "xetaJurnali.html"=>"MIS_ERROR_LOG"
}.freeze

def props(path)
  File.readlines(path,encoding:"UTF-8").each_with_object({}) do |line,result|
    next if line.strip.empty?||line.start_with?("#","!")
    key,value=line.chomp.split("=",2);result[key]=value if value
  end
end
def sql(value);"'#{value.to_s.gsub("'","''").gsub('\\n',"\n")}'";end

catalogs=%w[az en ru].to_h{|locale|[locale,props(File.join(RES,"messages_#{locale}.properties"))]}
keys=catalogs.fetch("az").keys.to_set
usages=Hash.new{|h,k|h[k]=Set.new}
Dir[File.join(TEMPLATES,"**/*.html")].each do |path|
  relative=path.delete_prefix(TEMPLATES+"/")
  module_code=if relative.start_with?("fragments/","layouts/","error/") then "GLOBAL" else PAGE_MODULE[File.basename(path)] end
  next unless module_code
  File.read(path,encoding:"UTF-8").scan(/\#\{([a-z][a-z0-9_.]+)(?:\([^}]*\))?\}/i) do |match|
    key=match.first;usages[key]<<[module_code,relative] if keys.include?(key)
  end
end
keys.grep(/\Amenu\.module\./).each do |key|
  code=key.delete_prefix("menu.module.").upcase;usages[key]<<[code,"fragments/sidebar.html"]
end
keys.each{|key|usages[key]<<["GLOBAL","model/common"] if usages[key].empty?}

translation_rows=catalogs.flat_map{|locale,entries|entries.map{|key,value|[locale,key,value]}}
usage_rows=usages.flat_map{|key,places|places.map{|modul,screen|[key,modul,screen]}}

translation_values=translation_rows.map{|locale,key,value|"(#{sql(locale)},#{sql(key)},#{sql(value)})"}.join(",\n")
usage_values=usage_rows.map{|key,modul,screen|"(#{sql(key)},#{sql(modul)},#{sql(screen)})"}.join(",\n")
sql_text=<<~SQL
  CREATE TABLE IF NOT EXISTS public.kn_tercume_modul_elaqeleri (
      id bigserial PRIMARY KEY,
      acar varchar(200) NOT NULL,
      modul_kodu varchar(100) NOT NULL,
      ekran varchar(300) NOT NULL,
      yaranma_tarixi timestamp NOT NULL DEFAULT now(),
      CONSTRAINT uq_kn_tercume_modul_elaqeleri UNIQUE(acar,modul_kodu,ekran)
  );
  CREATE INDEX IF NOT EXISTS ix_kn_tercume_modul_elaqeleri_modul ON public.kn_tercume_modul_elaqeleri(modul_kodu,acar);

  WITH data(dil_kodu,acar,deyer) AS (VALUES
  #{translation_values}
  )
  INSERT INTO public.kn_interfeys_tercumeleri(dil_id,acar,deyer)
  SELECT d.id,data.acar,data.deyer FROM data JOIN public.kn_diller d ON d.kod=data.dil_kodu
  ON CONFLICT(dil_id,acar) DO NOTHING;

  INSERT INTO public.kn_tercume_modul_elaqeleri(acar,modul_kodu,ekran) VALUES
  #{usage_values}
  ON CONFLICT(acar,modul_kodu,ekran) DO NOTHING;
SQL
File.write(File.join(RES,"db/migration/V27__add_department_service_organization_error_translations.sql"),sql_text,encoding:"UTF-8")
puts "#{keys.size} açar, #{translation_rows.size} dil sətri və #{usage_rows.size} modul əlaqəsi hazırlandı."
