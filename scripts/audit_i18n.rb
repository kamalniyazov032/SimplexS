#!/usr/bin/env ruby
require "nokogiri"
require "set"
ROOT=File.expand_path("..",__dir__)
issues=[]
translated_pages=Set.new(%w[binaParametrleri.html binalar.html diaqnozlar.html emekdash.html kassalar.html login.html modullar.html muhasibatKodu.html rollar.html shobeXidmet.html tercumeler.html vezifeler.html xidmetQiymetleri.html xidmetQruplari.html yataqlar.html shobe.html xidmet.html teskilatlar.html xetaJurnali.html])
Dir[File.join(ROOT,"src/main/resources/templates/**/*.html")].each do |path|
  relative=path.sub(File.join(ROOT,"src/main/resources/templates/")+"","")
  next unless relative.start_with?("fragments/","layouts/","error/") || (relative.start_with?("pages/") && translated_pages.include?(File.basename(relative)))
  doc=Nokogiri::HTML(File.read(path,encoding:"UTF-8"))
  doc.xpath("//text()").each do |node|
    text=node.text.strip.gsub(/\s+/," ");next unless text.match?(/[[:alpha:]]/)
    ancestors=node.ancestors;next if ancestors.any?{|a|%w[script style].include?(a.name)||a.key?("th:text")}
    next if text.include?('${')||text.include?('#{')||text.include?('@{')||text.start_with?("<!--")
    issues << "#{path.sub(ROOT+'/','')}: mətn: #{text[0,90]}"
  end
  doc.xpath("//*[@placeholder or @title or @aria-label or @data-placeholder]").each do |node|
    {"placeholder"=>"th:placeholder","title"=>"th:title","aria-label"=>"th:aria-label","data-placeholder"=>"th:attr"}.each do |plain,dynamic|
      value=node[plain];next if value.nil?||!value.match?(/[[:alpha:]]/)||node.key?(dynamic)
      next if plain=="placeholder" && value.match?(/\Ati(?: ti-[a-z0-9-]+)+\z/i)
      next if relative=="pages/tercumeler.html" && plain=="placeholder" && %w[en English İngilis\ dili].include?(value)
      issues << "#{path.sub(ROOT+'/','')}: atribut #{plain}: #{value}"
    end
  end
end
if issues.empty?
  puts "HTML i18n auditi uğurludur."
else
  warn issues.join("\n");warn "Cəmi: #{issues.size}";exit 1
end
