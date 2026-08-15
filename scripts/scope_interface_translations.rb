#!/usr/bin/env ruby
# Tərcümə əhatəsini yalnız hazır Parametrlər və Personal ekranları ilə məhdudlaşdırır.
require "set"

ROOT = File.expand_path("..", __dir__)
TEMPLATES = File.join(ROOT, "src/main/resources/templates")
RESOURCES = File.join(ROOT, "src/main/resources")

TRANSLATED_PAGES = Set.new(%w[
  binaParametrleri.html
  binalar.html
  diaqnozlar.html
  shobe.html
  xidmet.html
  teskilatlar.html
  xetaJurnali.html
  emekdash.html
  kassalar.html
  login.html
  modullar.html
  muhasibatKodu.html
  rollar.html
  shobeXidmet.html
  tercumeler.html
  vezifeler.html
  xidmetQiymetleri.html
  xidmetQruplari.html
  yataqlar.html
]).freeze

def translated_template?(path)
  relative = path.delete_prefix(TEMPLATES + "/")
  return true if relative.start_with?("fragments/", "layouts/", "error/")
  relative.start_with?("pages/") && TRANSLATED_PAGES.include?(File.basename(relative))
end

def remove_i18n_attributes(source)
  source
    .gsub(/\s+th:(?:text|placeholder|title|aria-label)="\#\{ui\.[^}"]+\}"/, "")
    .gsub(/\s+th:attr="data-placeholder=\#\{ui\.[^}"]+\}"/, "")
    .gsub(/\s+th:attr="[^"]*\#\{ui\.[^"]+"/, "")
end

files = Dir[File.join(TEMPLATES, "**/*.html")]
files.reject { |path| translated_template?(path) }.each do |path|
  source = File.read(path, encoding: "UTF-8")
  changed = remove_i18n_attributes(source)
  File.write(path, changed, encoding: "UTF-8") if changed != source
end

# Dəyişməyən texniki ikon kodları dil kataloquna daxil edilmir.
files.each do |path|
  source = File.read(path, encoding: "UTF-8")
  changed = source.gsub(/\s+th:placeholder="\#\{ui\.ti_ti_(?:users|settings)\.[^}"]+\}"/, "")
  File.write(path, changed, encoding: "UTF-8") if changed != source
end

keys = Set.new
files.select { |path| translated_template?(path) }.each do |path|
  File.read(path, encoding: "UTF-8").scan(/\#\{([a-z][a-z0-9_.]+)(?:\([^}]*\))?\}/i) { |match| keys << match.first }
end

# Java/model səviyyəsində lokallaşdırılan ümumi başlıq və bildirişləri də saxla.
az_path = File.join(RESOURCES, "messages_az.properties")
az_lines = File.readlines(az_path, encoding: "UTF-8")
az_values = az_lines.map do |line|
  match = line.match(/\A([^#!\s][^=]*)=(.*)\z/)
  [match[1], match[2].chomp] if match
end.compact.to_h

common_model_values = Set.new(%w[
  Uğurlu Əməliyyat Xəta Diqqət
])
az_values.each { |key, value| keys << key if common_model_values.include?(value) }
az_values.each_key { |key| keys << key if key.start_with?("menu.") }

%w[messages.properties messages_az.properties messages_en.properties messages_ru.properties].each do |name|
  path = File.join(RESOURCES, name)
  kept = File.readlines(path, encoding: "UTF-8").select do |line|
    stripped = line.strip
    stripped.empty? || stripped.start_with?("#", "!") || keys.include?(line.split("=", 2).first)
  end
  File.write(path, kept.join, encoding: "UTF-8")
end

sql_keys = keys.sort.map { |key| "    '#{key.gsub("'", "''")}'" }.join(",\n")
migration = <<~SQL
  -- Yalnız hazır Parametrlər və Personal ekranlarının interfeys tərcümələri saxlanılır.
  DELETE FROM public.kn_interfeys_tercumeleri
  WHERE acar NOT IN (
  #{sql_keys}
  );
SQL
File.write(File.join(RESOURCES, "db/migration/V24__remove_dynamic_and_technical_translation_keys.sql"), migration, encoding: "UTF-8")

puts "#{keys.size} açar saxlanıldı; digər səhifələr statik Azərbaycan mətninə qaytarıldı."
