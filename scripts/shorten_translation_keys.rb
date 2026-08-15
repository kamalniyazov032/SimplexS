#!/usr/bin/env ruby
require "digest"
require "set"

ROOT=File.expand_path("..",__dir__)
RES=File.join(ROOT,"src/main/resources")
TEMPLATES=File.join(RES,"templates")
PAGE_MODULE={
  "emekdash.html"=>"personal", "rollar.html"=>"roles", "modullar.html"=>"modules",
  "xidmetQiymetleri.html"=>"prices", "yataqlar.html"=>"beds", "diaqnozlar.html"=>"diagnoses",
  "xidmetQruplari.html"=>"service_groups", "vezifeler.html"=>"positions", "kassalar.html"=>"cash_registers",
  "binalar.html"=>"buildings", "muhasibatKodu.html"=>"account_codes", "shobeXidmet.html"=>"department_services",
  "binaParametrleri.html"=>"building_params", "tercumeler.html"=>"translations", "login.html"=>"common"
}.freeze

def props(path)
  File.readlines(path,encoding:"UTF-8").map do |line|
    next if line.strip.empty?||line.start_with?("#","!")
    key,value=line.chomp.split("=",2);[key,value]
  end.compact.to_h
end
def sql(value);"'#{value.gsub("'","''")}'";end

az=props(File.join(RES,"messages_az.properties"))
usages=Hash.new{|h,k|h[k]=Set.new}
Dir[File.join(TEMPLATES,"**/*.html")].each do |path|
  relative=path.delete_prefix(TEMPLATES+"/")
  scope=relative.start_with?("fragments/","layouts/","error/") ? "common" : PAGE_MODULE[File.basename(path)]
  next unless scope
  File.read(path,encoding:"UTF-8").scan(/\#\{([a-z][a-z0-9_.]+)(?:\([^}]*\))?\}/i){|m|usages[m.first]<<scope if az.key?(m.first)}
end

reserved=az.keys.reject{|key|key.start_with?("ui.")}.to_set
mapping={}
az.keys.select{|key|key.start_with?("ui.")}.sort.each do |old|
  scopes=usages[old]
  scope=scopes.size==1 ? scopes.first : "common"
  raw=old.delete_prefix("ui.").sub(/\.[0-9a-f]{6}\z/,"")
  max_slug=[48-scope.length-1,12].max
  slug=raw[0,max_slug].sub(/_+\z/,"")
  candidate="#{scope}.#{slug}"
  if reserved.include?(candidate)||mapping.value?(candidate)
    suffix=Digest::SHA1.hexdigest(old)[0,4]
    slug=slug[0,[max_slug-5,8].max].sub(/_+\z/,"")
    candidate="#{scope}.#{slug}_#{suffix}"
  end
  raise "Açar 48 simvoldan uzundur: #{candidate}" if candidate.length>48
  mapping[old]=candidate
end

Dir[File.join(TEMPLATES,"**/*.html")].each do |path|
  source=File.read(path,encoding:"UTF-8");changed=source.dup
  mapping.each{|old,new_key|changed.gsub!(old,new_key)}
  File.write(path,changed,encoding:"UTF-8") if changed!=source
end

%w[messages.properties messages_az.properties messages_en.properties messages_ru.properties].each do |name|
  path=File.join(RES,name);lines=File.readlines(path,encoding:"UTF-8")
  changed=lines.map do |line|
    key,value=line.chomp.split("=",2);value&&mapping.key?(key) ? "#{mapping[key]}=#{value}\n" : line
  end
  File.write(path,changed.join,encoding:"UTF-8")
end

values=mapping.map{|old,new_key|"(#{sql(old)},#{sql(new_key)})"}.join(",\n")
migration=<<~SQL
  -- Uzun avtomatik UI açarlarını <modul>.<qısa_məna> standartına keçirir.
  CREATE TEMP TABLE tmp_tercume_acar_xeritesi(old_acar varchar(200),new_acar varchar(200)) ON COMMIT DROP;
  INSERT INTO tmp_tercume_acar_xeritesi(old_acar,new_acar) VALUES
  #{values};

  UPDATE public.kn_interfeys_tercumeleri t
  SET acar=x.new_acar,yenilenme_tarixi=now()
  FROM tmp_tercume_acar_xeritesi x
  WHERE t.acar=x.old_acar;

  UPDATE public.kn_tercume_modul_elaqeleri e
  SET acar=x.new_acar
  FROM tmp_tercume_acar_xeritesi x
  WHERE e.acar=x.old_acar;
SQL
File.write(File.join(RES,"db/migration/V26__short_translation_keys.sql"),migration,encoding:"UTF-8")
puts "#{mapping.size} uzun açar qısaldıldı; maksimum uzunluq #{mapping.values.map(&:length).max}."
