#!/usr/bin/env ruby
ROOT=File.expand_path("..",__dir__)
def props(path)
  File.readlines(path,encoding:"UTF-8").reject{|l|l.strip.empty?||l.start_with?("#","!")}.to_h{|line|line.chomp.split("=",2)}
end
def sql(value);"'#{value.gsub("'","''").gsub('\\n',"\n")}'";end
rows=%w[az en ru].flat_map do |locale|
  props(File.join(ROOT,"src/main/resources/messages_#{locale}.properties")).map{|key,value|[locale,key,value]}
end
parts=[<<~SQL]
  INSERT INTO public.kn_diller(kod,ad,yerli_ad,standartdir,aktiv,sira_no) VALUES
  ('en','İngilis dili','English',false,true,2),
  ('ru','Rus dili','Русский',false,true,3)
  ON CONFLICT(kod) DO UPDATE SET ad=EXCLUDED.ad,yerli_ad=EXCLUDED.yerli_ad,aktiv=true,sira_no=EXCLUDED.sira_no;
SQL
rows.each_slice(250) do |batch|
  values=batch.map{|locale,key,value|"(#{sql(locale)},#{sql(key)},#{sql(value)})"}.join(",\n")
  parts<<"""
    WITH data(dil_kodu,acar,deyer) AS (VALUES
    #{values}
    )
    INSERT INTO public.kn_interfeys_tercumeleri(dil_id,acar,deyer)
    SELECT d.id,data.acar,data.deyer FROM data JOIN public.kn_diller d ON d.kod=data.dil_kodu
    ON CONFLICT(dil_id,acar) DO UPDATE SET deyer=EXCLUDED.deyer,yenilenme_tarixi=now();
  """
end
File.write(File.join(ROOT,"src/main/resources/db/migration/V19__seed_az_en_ru_interface_translations.sql"),parts.join("\n"),encoding:"UTF-8")
puts "#{rows.size} tərcümə sətri üçün V19 yaradıldı."
